package com.nithishramurthy.farm.fresh.service;

import com.nithishramurthy.farm.fresh.dto.admin.RoleChangeView;
import com.nithishramurthy.farm.fresh.dto.admin.UserView;
import com.nithishramurthy.farm.fresh.entity.Role;
import com.nithishramurthy.farm.fresh.entity.RoleChange;
import com.nithishramurthy.farm.fresh.entity.User;
import com.nithishramurthy.farm.fresh.exception.NotAllowedException;
import com.nithishramurthy.farm.fresh.exception.ResourceNotFoundException;
import com.nithishramurthy.farm.fresh.repository.RoleChangeRepository;
import com.nithishramurthy.farm.fresh.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * Who may give whom which role:
 * <ul>
 *   <li>the owner can make anyone a user, member, admin or owner;</li>
 *   <li>an admin can only move people between user and member;</li>
 *   <li>nobody changes their own role, and the last owner can't be demoted.</li>
 * </ul>
 * Every change is written to the role_changes log.
 */
@Service
public class UserManagementService {

    private final UserRepository userRepository;
    private final RoleChangeRepository roleChangeRepository;

    public UserManagementService(UserRepository userRepository, RoleChangeRepository roleChangeRepository) {
        this.userRepository = userRepository;
        this.roleChangeRepository = roleChangeRepository;
    }

    /** Everyone, optionally filtered by name or email, with what the viewer may change. */
    @Transactional(readOnly = true)
    public List<UserView> listUsers(User viewer, String query) {
        String term = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return userRepository.findAllByOrderByIdAsc().stream()
                .filter(u -> term.isEmpty()
                        || u.getEmail().toLowerCase(Locale.ROOT).contains(term)
                        || (u.getName() != null && u.getName().toLowerCase(Locale.ROOT).contains(term)))
                .map(u -> UserView.from(u, viewer))
                .toList();
    }

    @Transactional
    public UserView changeRole(Long userId, Role next, User actor) {
        if (userId.equals(actor.getId())) {
            throw new NotAllowedException("You can't change your own role.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        Role current = user.roleEnum();
        if (current == next) {
            return UserView.from(user, actor);
        }
        if (!actor.roleEnum().canChange(current, next)) {
            throw new NotAllowedException(actor.roleEnum() == Role.ADMIN
                    ? "Admins can only move people between user and member. Ask the owner."
                    : "You can't change this person's role.");
        }
        if (current == Role.OWNER && userRepository.countByRole(Role.OWNER.authority()) <= 1) {
            throw new IllegalStateException("The shop needs at least one owner.");
        }

        user.setRole(next.authority());
        User saved = userRepository.save(user);
        roleChangeRepository.save(new RoleChange(saved, current, next, actor.getEmail()));
        return UserView.from(saved, actor);
    }

    /** The 50 most recent role changes. */
    @Transactional(readOnly = true)
    public List<RoleChangeView> recentChanges() {
        return roleChangeRepository.findTop50ByOrderByChangedAtDescIdDesc().stream()
                .map(RoleChangeView::from)
                .toList();
    }
}
