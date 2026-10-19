/** Chưa có ảnh: chữ cái đầu của display name (FR-05.1, vd. "Huy" -> "H"). */
function initial(name = '') {
  return name.trim().charAt(0).toUpperCase();
}

export function Avatar({ user, size = 36 }) {
  const style = { width: size, height: size, fontSize: Math.round(size * 0.4) };
  if (user.avatarUrl) {
    return <img className="avatar" style={style} src={user.avatarUrl} alt={user.displayName} referrerPolicy="no-referrer" />;
  }
  return (
    <span className="avatar avatar-fallback" style={style} aria-hidden="true">
      {initial(user.displayName)}
    </span>
  );
}
