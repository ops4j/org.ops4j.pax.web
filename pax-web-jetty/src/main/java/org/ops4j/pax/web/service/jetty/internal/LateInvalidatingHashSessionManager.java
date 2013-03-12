package org.ops4j.pax.web.service.jetty.internal;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import javax.servlet.http.HttpSession;

import org.eclipse.jetty.server.session.AbstractSession;
import org.eclipse.jetty.server.session.HashSessionIdManager;
import org.eclipse.jetty.server.session.HashSessionManager;
import org.eclipse.jetty.server.session.HashedSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hash session manager extension that scavenges sessions only when all sessions
 * with the same session id are ready for scavenge.
 * 
 * @author Marc Klinger - mklinger[at]nightlabs[dot]de
 */
public class LateInvalidatingHashSessionManager extends HashSessionManager {
	private static final Logger LOG = LoggerFactory
			.getLogger(LateInvalidatingHashSessionManager.class);

	/**
	 * This is a hack that sets the accessed and lastAccessed fields in
	 * HashedSession for all sessions with the same id when not all sessions are
	 * ready to expire.
	 */
	@Override
	protected void scavenge() {
		// don't attempt to scavenge if we are shutting down
		if (isStopping() || isStopped()) {
			return;
		}

		final Thread thread = Thread.currentThread();
		final ClassLoader oldClazzLoader = thread.getContextClassLoader();
		try {
			if (_loader != null) {
				thread.setContextClassLoader(_loader);
			}

			synchronized (LateInvalidatingHashSessionManager.class) {
				final long now = System.currentTimeMillis();
				for (final HashedSession session : _sessions.values()) {
					final long idleTime = session.getMaxInactiveInterval() * 1000;
					if (isTimeoutCandidate(session, idleTime, now)) {
						if (_sessionIdManager instanceof HashSessionIdManager) {
							final Collection<AbstractSession> sessionsWithId = getSessionsWithId(session
									.getId());
							if (sessionsWithId == null
									|| sessionsWithId.size() < 1) {
								throw new IllegalStateException();
							}
							if (areAllTimeoutCandidates(sessionsWithId,
									idleTime, now)) {
								LOG.warn("Timing out for "
										+ sessionsWithId.size()
										+ " session(s) with id "
										+ session.getId());
								for (AbstractSession sessionToTimeout : sessionsWithId) {
									sessionTimeout(sessionToTimeout);
								}
							} else {
								LOG.warn("Extending timeout for "
										+ sessionsWithId.size()
										+ " session(s) with id "
										+ session.getId());
								setLatestLastAccessed(sessionsWithId);
							}
						} else {
							sessionTimeout(session);
						}
					} else if (getIdleSavePeriodMs() > 0
							&& session.getAccessed() + getIdleSavePeriodMs() < now) {
						session.idle();
					}
				}
			}
		} catch (final Throwable t) { //CHECKSTYLE:SKIP
			if (t instanceof ThreadDeath) {
				throw ((ThreadDeath) t);
			} else {
				LOG.warn("Problem scavenging sessions", t);
			}
		} finally {
			thread.setContextClassLoader(oldClazzLoader);
		}
	}

	private Collection<AbstractSession> getSessionsWithId(String id) {
		Collection<HttpSession> sessions = ((HashSessionIdManager) _sessionIdManager)
				.getSession(id);
		if (sessions == null) {
			return null;
		}
		if (sessions.isEmpty()) {
			return Collections.emptyList();
		}
		Collection<AbstractSession> abstractSessions = new ArrayList<AbstractSession>(
				sessions.size());
		for (HttpSession session : sessions) {
			abstractSessions.add((AbstractSession) session);
		}
		return abstractSessions;
	}

	private boolean areAllTimeoutCandidates(
			final Collection<AbstractSession> sessions, final long idleTime,
			final long now) {
		for (AbstractSession session : sessions) {
			if (!isTimeoutCandidate(session, idleTime, now)) {
				return false;
			}
		}
		return true;
	}

	private boolean isTimeoutCandidate(final AbstractSession session,
			final long idleTime, final long now) {
		return idleTime > 0 && session.getAccessed() + idleTime < now;
	}

	private long getIdleSavePeriodMs() {
		try {
			Field f = HashSessionManager.class
					.getDeclaredField("_idleSavePeriodMs");
			f.setAccessible(true);
			return (Long) f.get(this);
		} catch (Exception e) { //CHECKSTYLE:SKIP
			throw new RuntimeException(
					"Error accessing invisible HashSessionManager field via reflection",
					e);
		}
	}

	private void sessionTimeout(AbstractSession session) {
		try {
			Method m = AbstractSession.class.getDeclaredMethod("timeout",
					new Class<?>[0]);
			m.setAccessible(true);
			m.invoke(session, new Object[0]);
		} catch (Exception e) { //CHECKSTYLE:SKIP
			throw new RuntimeException(
					"Error accessing invisible AbstractSession method via reflection",
					e);
		}
	}

	private void setLatestLastAccessed(
			final Collection<AbstractSession> sessionsWithId) {
		long latestAccessed = 0;
		long latestLastAccessed = 0;
		for (final AbstractSession otherSession : sessionsWithId) {
			latestAccessed = Math.max(latestAccessed,
					otherSession.getAccessed());
			latestLastAccessed = Math.max(latestLastAccessed,
					otherSession.getLastAccessedTime());
		}
		for (final AbstractSession session : sessionsWithId) {
			if (session.getAccessed() < latestAccessed) {
				try {
					final Field accessedField = AbstractSession.class
							.getDeclaredField("_accessed");
					accessedField.setAccessible(true);
					accessedField.set(session, latestAccessed);
				} catch (final Exception e) {
					LOG.warn("Error setting _accessed for session " + session,
							e);
				}
			}
			if (session.getLastAccessedTime() < latestLastAccessed) {
				try {
					final Field lastAccessedField = AbstractSession.class
							.getDeclaredField("_lastAccessed");
					lastAccessedField.setAccessible(true);
					lastAccessedField.set(session, latestLastAccessed);
				} catch (final Exception e) {
					LOG.warn("Error setting _lastAccessed for session "
							+ session, e);
				}
			}
		}
	}

	/**
	 * The change in this method allows sessions to be saved when there are
	 * multiple sessions with the same session id.
	 */
	@Override
	protected void invalidateSessions() throws Exception {
		// Invalidate all sessions to cause unbind events
		ArrayList<HashedSession> sessions = new ArrayList<HashedSession>(
				_sessions.values());
		int loop = 100;
		while (sessions.size() > 0 && loop-- > 0) {
			// If we are called from doStop
			if (isStopping()) {
				// Then we only save and remove the session - it is not
				// invalidated.
				File storeDir = getStoreDir(this);
				for (HashedSession session : sessions) {
					if (storeDir != null && storeDir.exists()
							&& storeDir.canWrite()) {
						sessionSave(session, false);
					}
					removeSession(session, false);
				}
			} else {
				for (HashedSession session : sessions) {
					session.invalidate();
				}
			}

			// check that no new sessions were created while we were iterating
			sessions = new ArrayList<HashedSession>(_sessions.values());
		}
	}

	private File getStoreDir(HashSessionManager manager) {
		try {
			Field f = HashSessionManager.class.getDeclaredField("_storeDir");
			f.setAccessible(true);
			return (File) f.get(this);
		} catch (Exception e) { //CHECKSTYLE:SKIP
			throw new RuntimeException(
					"Error accessing invisible HashSessionManager field via reflection",
					e);
		}
	}

	private void sessionSave(HashedSession session, boolean reactivate) {
		try {
			Method m = HashedSession.class.getDeclaredMethod("save",
					new Class<?>[] { Boolean.TYPE });
			m.setAccessible(true);
			m.invoke(session, new Object[] { reactivate });
		} catch (Exception e) { //CHECKSTYLE:SKIP
			throw new RuntimeException(
					"Error accessing invisible HashedSession method via reflection",
					e);
		}
	}
}
