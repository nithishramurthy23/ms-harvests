import { useEffect, useState } from "react";
import { useAuth } from "../../context/AuthContext";
import { ROLES, isOwner, roleLabel } from "../../utils/roles";
import { adminApi, errorMessage } from "./adminApi";

// People and roles. The owner can give anyone any role; an admin can only move
// customers between user and member. The backend decides this and sends each
// person's assignableRoles, so the page only offers what will be accepted.
export default function PeoplePage() {
  const { user: me } = useAuth();
  const [query, setQuery] = useState("");
  const [users, setUsers] = useState(null);
  const [changes, setChanges] = useState([]);
  const [error, setError] = useState("");
  const [saved, setSaved] = useState("");

  useEffect(() => {
    const timer = setTimeout(() => {
      adminApi.users(query.trim()).then(setUsers).catch((err) => setError(errorMessage(err, "Couldn't load people.")));
    }, 250);
    return () => clearTimeout(timer);
  }, [query]);

  const loadChanges = () => adminApi.roleChanges().then(setChanges).catch(() => setChanges([]));
  useEffect(() => {
    loadChanges();
  }, []);

  const changeRole = async (person, role) => {
    setError("");
    setSaved("");
    try {
      const updated = await adminApi.setRole(person.id, role);
      setUsers((list) => list.map((u) => (u.id === updated.id ? updated : u)));
      setSaved(`${updated.name || updated.email} is now ${roleLabel(updated.role).toLowerCase()}.`);
      loadChanges();
    } catch (err) {
      setError(errorMessage(err));
    }
  };

  return (
    <section className="space-y-8">
      <div>
        <p className="mb-4 text-sm text-bark/65">
          {isOwner(me)
            ? "You can make anyone a user, member, admin or owner."
            : "You can move customers between user and member. Only the owner can add or remove admins."}
        </p>
        <div className="mb-4 grid gap-2 text-sm text-bark/65 sm:grid-cols-2 lg:grid-cols-4">
          {ROLES.map((r) => (
            <p key={r.value}>
              <strong className="text-bark">{r.label}:</strong> {r.hint}
            </p>
          ))}
        </div>

        <label className="sr-only" htmlFor="people-search">
          Search people
        </label>
        <input
          id="people-search"
          type="search"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Search by name or email"
          className="mb-4 h-10 w-full rounded-full border border-bark/15 bg-cream px-4 text-sm sm:max-w-sm"
        />

        {error && <p className="mb-4 text-sm text-clay-600">{error}</p>}
        {saved && (
          <p className="mb-4 text-sm text-green-700" role="status">
            {saved}
          </p>
        )}

        {!users ? (
          <p className="text-bark/60">Loading…</p>
        ) : users.length === 0 ? (
          <p className="text-bark/60">Nobody matches “{query}”.</p>
        ) : (
          <ul className="divide-y divide-bark/10 rounded-2xl border border-bark/10 bg-paper">
            {users.map((person) => {
              const options = person.assignableRoles.includes(person.role)
                ? person.assignableRoles
                : [person.role, ...person.assignableRoles];
              const locked = person.assignableRoles.length === 0;
              return (
                <li key={person.id} className="flex flex-wrap items-center gap-3 px-4 py-3">
                  <div className="min-w-[12rem] flex-1">
                    <p className="font-medium">
                      {person.name}
                      {person.id === me?.id && <span className="ml-2 text-xs text-bark/50">(you)</span>}
                      {!person.verified && <span className="ml-2 text-xs text-bark/50">not verified</span>}
                    </p>
                    <p className="break-all text-sm text-bark/55">{person.email}</p>
                  </div>
                  {locked ? (
                    <span className="rounded-full bg-bark/5 px-4 py-2 text-sm">{roleLabel(person.role)}</span>
                  ) : (
                    <>
                      <label className="sr-only" htmlFor={`role-${person.id}`}>
                        Role for {person.email}
                      </label>
                      <select
                        id={`role-${person.id}`}
                        value={person.role}
                        onChange={(e) => changeRole(person, e.target.value)}
                        className="h-10 rounded-full border border-bark/15 bg-cream px-4 text-sm"
                      >
                        {options.map((value) => (
                          <option key={value} value={value}>
                            {roleLabel(value)}
                          </option>
                        ))}
                      </select>
                    </>
                  )}
                </li>
              );
            })}
          </ul>
        )}
      </div>

      <div>
        <h2 className="mb-3 text-xl text-green-900">Recent role changes</h2>
        {changes.length === 0 ? (
          <p className="text-bark/60">No changes yet.</p>
        ) : (
          <ul className="divide-y divide-bark/10 rounded-2xl border border-bark/10 bg-paper text-sm">
            {changes.map((c) => (
              <li key={c.id} className="flex flex-wrap justify-between gap-2 px-4 py-3">
                <span>
                  <strong>{c.userName || c.userEmail}</strong>: {roleLabel(c.fromRole)} → {roleLabel(c.toRole)}
                </span>
                <span className="text-bark/55">
                  by {c.changedBy}, {new Date(c.changedAt).toLocaleString("en-IN")}
                </span>
              </li>
            ))}
          </ul>
        )}
      </div>
    </section>
  );
}
