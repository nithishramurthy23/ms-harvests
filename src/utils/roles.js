// What the logged-in user's role lets them see. The backend enforces the same
// rules (SecurityConfig); these only decide what to show.
export const canManageStore = (user) => user?.role === "ADMIN" || user?.role === "OWNER";
export const isOwner = (user) => user?.role === "OWNER";

export const ROLES = [
  { value: "USER", label: "User", hint: "Shops" },
  { value: "MEMBER", label: "Member", hint: "Shops (member perks later)" },
  { value: "ADMIN", label: "Admin", hint: "Products, stock, orders and members" },
  { value: "OWNER", label: "Owner", hint: "Everything, plus who is an admin" },
];

export const roleLabel = (value) => ROLES.find((r) => r.value === value)?.label ?? value;
