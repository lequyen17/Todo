import { Link } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { Avatar } from '../components/Avatar';
import { BadgeIcon, CalendarIcon, ClockIcon, MailIcon, ShieldCheckIcon, UserIcon } from '../components/Icons';
import { formatDate } from '../utils/format';

function InfoRow({ icon, label, children }) {
  return (
    <div className="info-row">
      <span className="info-row-icon">{icon}</span>
      <div>
        <div className="info-row-label">{label}</div>
        <div className="info-row-value">{children}</div>
      </div>
    </div>
  );
}

export default function ProfilePage() {
  const { user } = useAuth();

  return (
    <div className="page">
      <section className="card profile-hero">
        <div className="profile-cover" />
        <div className="profile-hero-body">
          <div className="profile-avatar-ring">
            <Avatar user={user} size={96} />
          </div>
          <div className="profile-hero-text">
            <h1>{user.displayName}</h1>
            <p className="muted">{user.email}</p>
            <div className="badges">
              <span className="badge">{user.role === 'ADMIN' ? 'Admin' : 'Member'}</span>
              {user.emailVerifiedAt && (
                <span className="badge badge-success">
                  <ShieldCheckIcon size={14} /> Email verified
                </span>
              )}
            </div>
          </div>
        </div>
      </section>

      <div className="profile-grid">
        <section className="card">
          <h2 className="card-title">Personal information</h2>
          <InfoRow icon={<UserIcon />} label="Display name">
            {user.displayName}
          </InfoRow>
          <InfoRow icon={<MailIcon />} label="Email">
            {user.email}
          </InfoRow>
          <InfoRow icon={<BadgeIcon />} label="Role">
            {user.role === 'ADMIN' ? 'Administrator' : 'Member'}
          </InfoRow>
        </section>

        <section className="card">
          <h2 className="card-title">Account activity</h2>
          <InfoRow icon={<CalendarIcon />} label="Member since">
            {formatDate(user.createdAt)}
          </InfoRow>
          <InfoRow icon={<ShieldCheckIcon />} label="Email verified">
            {formatDate(user.emailVerifiedAt)}
          </InfoRow>
          <InfoRow icon={<ClockIcon />} label="Last sign-in">
            {formatDate(user.lastLoginAt)}
          </InfoRow>
        </section>
      </div>

      <p className="muted page-hint">
        Want to change your password? Go to <Link to="/settings">Settings</Link>.
      </p>
    </div>
  );
}
