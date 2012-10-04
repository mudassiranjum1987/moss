/*
 *	MSemaphore.java -- user-level semaphore for MOSS
 *	Copyright (C) 2004 Fred Barnes <frmb@kent.ac.uk>
 *
 *	This program is free software; you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation; either version 2 of the License, or
 *	(at your option) any later version.
 *
 *	This program is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public License
 *	along with this program; if not, write to the Free Software
 *	Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package moss.ipc;

import moss.kernel.*;
import moss.user.*;

import java.util.*;

/**
 * this class implements a "semaphore".  Operations are performed based
 * on changing the semaphore's value arbitrarily, not strictly `wait' (-1)
 * and `signal' (+1).
 *
 * However, wait() and signal() methods are still provided, for bits of
 * the kernel that want to use them like that.
 */

public class MSemaphore
{
	//{{{  private things
	/** lock for private variables */
	private static Object synclock;

	/** hash-table of semaphores */
	private static Hashtable sems;

	/** semaphore structure.  The value is never below zero.  */
	private static class MSem {
		public int key;
		public int value;
		public MWaitQueue waiting;
		public boolean destroyed;
	}
	
	//}}}
	
	//{{{  public constants
	/** create a semaphore */
	public static final int SEMOP_CREATE = 0;
	/** set (adjust) a semaphore's value */
	public static final int SEMOP_SET = 1;
	/** remove a semaphore */
	public static final int SEMOP_REMOVE = 2;
	//}}}

	//{{{  public static void init_semaphore ()
	/**
	 * this initialises the semaphore handling class
	 */
	public static void init_semaphore ()
	{
		synclock = new Object ();
		sems = new Hashtable ();

		sems.clear ();
		return;
	}
	//}}}
	//{{{  public static void finalinit ()
	/**
	 * this is called to perform any final initialisation,
	 * in the context of the init-task.
	 */
	public static void finalinit ()
	{
		return;
	}
	//}}}
	
	//{{{  private static MSem find_sem (int key)
	/**
	 * returns an MSem structure for a particular semaphore,
	 * based on "key".
	 *
	 * @param key semaphore key
	 *
	 * @return MSem reference if found, null otherwise
	 */
	private static MSem find_sem (int key)
	{
		Integer ki = new Integer (key);
		MSem sem;

		synchronized (synclock) {
			sem = (MSem)sems.get ((Object)ki);
		}
		return sem;
	}
	//}}}

	//{{{  public static int semcreate (int key, int value)
	/**
	 * creates a new semaphore
	 *
	 * @param key semaphore key
	 * @param value initial value of the semaphore.  If &lt; 0, the method blocks.
	 *
	 * @return 0 on success, or &lt; 0 indicating error (key already exists)
	 */
	public static int semcreate (int key, int value)
	{
		MSem sem;
		boolean do_sleep = false;

		synchronized (synclock) {
			Integer ki;

			sem = find_sem (key);
			if (sem != null) {
				return -MSystem.EEXISTS;
			}
			/* else create semaphore */
			sem = new MSem ();
			sem.key = key;
			if (value < 0) {
				sem.value = 0;
				do_sleep = true;
			} else {
				sem.value = value;
			}
			sem.waiting = new MWaitQueue ();
			sem.destroyed = false;
			ki = new Integer (key);
			sems.put ((Object)ki, (Object)sem);
		}
		if (do_sleep) {
			MProcess current = MKernel.current[MProcessor.currentCPU()];

			return semset (current, key, value);
		}
		return 0;
	}
	//}}}
	//{{{  public static int semset (MProcess current, int key, int value)
	/**
	 * changes a semaphore's value
	 *
	 * @param key semaphore key
	 * @param value change semaphore value by this (+ve or -ve)
	 *
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public static int semset (MProcess current, int key, int value)
	{
		MSem sem;
		boolean do_sleep = false;
		int orig_value = value;

		sem = find_sem (key);
		if (sem == null) {
			return -MSystem.ENXIO;
		}

		/* else deal with semaphore value */
		synchronized (synclock) {
			if (value > 0) {
				/* increment value */
				sem.value += value;

				/* if there are any waiting processes, wake them up -- they will loop if not available */
				while (!sem.waiting.is_empty ()) {
					MProcess p = sem.waiting.get_from_queue ();

					MKernel.add_to_run_queue (p);
				}
			} else {
				do_sleep = true;
			}
		}

		if (do_sleep) {
			/* try and adjust value, sleep if not */
			while (value < 0) {

				synchronized (synclock) {
					/* adjust value */
					if (sem.value < (-value)) {
						value += sem.value;
						sem.value = 0;
					} else {
						sem.value += value;
						value = 0;
					}
				}

				if (value < 0) {
					//{{{  sleep if still more
					synchronized (current) {
						if (current.signalled) {
							do_sleep = false;
						} else {
							do_sleep = true;
							synchronized (synclock) {
								sem.waiting.add_to_queue (current);
							}
							current.state = MProcess.TASK_SLEEPING;
						}
					}
					if (do_sleep) {
						MKernel.schedule ();
					}
					/* check to see if we woke because the semaphore was being removed */
					if (sem.destroyed) {
						return -MSystem.ENOENT;		/* it'll do.. */
					}
					/* if signalled, better "undo" the operation */
					if (current.signalled) {
						synchronized (synclock) {
							if (do_sleep) {
								sem.waiting.del_from_queue (current);
							}

							/* and fix semaphore value */
							sem.value += (value - orig_value);
							/* but that might have woken someone else up..! (better schedule them if so) */
							while (!sem.waiting.is_empty ()) {
								MProcess p = sem.waiting.get_from_queue ();

								MKernel.add_to_run_queue (p);
							}
						}
						return -MSystem.EINTR;			/* interrupted! */
					}
					//}}}
				}
			}
		}
		return 0;

	}
	//}}}
	//{{{  public static int semremove (int key)
	/**
	 * removes a semaphore.  Blocked processes are woken up -- they return with ENOENT
	 *
	 * @param key semaphore key
	 *
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public static int semremove (int key)
	{
		MSem sem;

		synchronized (synclock) {
			Integer ki;

			sem = find_sem (key);
			if (sem == null) {
				return -MSystem.ENOENT;
			}
			if (sem.destroyed) {
				return -MSystem.ENOENT;
			}
			sem.destroyed = true;
			ki = new Integer (key);

			sems.remove ((Object)ki);		/* remove from sems */

			/* if waiting processes, resume them */
			while (!sem.waiting.is_empty ()) {
				MProcess p = sem.waiting.get_from_queue ();

				MKernel.add_to_run_queue (p);
			}
		}

		return 0;
	}
	//}}}
}

