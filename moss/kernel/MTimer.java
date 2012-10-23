/*
 *	MTimer.java -- provides a timeout mechanism for MOSS
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

package moss.kernel;

import moss.user.*;

/**
 * this class is used to provide process timeouts in MOSS.
 *
 * The implementation is somewhat grim, but without a mechanism for absolute time
 * in milli-seconds, the one-thread implementation would suffer incremental-error problems
 * (Java's timeout is "more or less" ...)
 */

public class MTimer
{
	//{{{  private vars
	/** threads must synchronized() on this before accessing the timer queue */
	private static Object synclock;

	/** thread handles */
	private static TimerThread timers[];
	/** process handles */
	private static MProcess procs[];
	//}}}

	//{{{  private static class TimerThread extends Thread
	private static class TimerThread extends Thread
	{
		private int tidx;
		private long timeout;

		public TimerThread (int idx, long timeout)
		{
			tidx = idx;
			this.timeout = timeout;
		}

		public void run ()
		{
			MProcess p;

			synchronized (this) {
				try {
					wait (timeout);
				} catch (InterruptedException e) {}
			}
			/* timed out */
			synchronized (synclock) {
				p = procs[tidx];
				/* free up slot */
				procs[tidx] = null;
				timers[tidx] = null;
			}
			if (p == null) {
				/* this timer was cancelled */
			} else {
				/* maybe reschedule process */
				synchronized (p) {
					if (p.state == MProcess.TASK_SLEEPING) {
						MKernel.add_to_run_queue (p);
					}
				}
			}
		}
	}
	//}}}

	//{{{  public static void init_timer ()
	/**
	 * called to initialise the timer mechanism
	 */
	public static void init_timer ()
	{
		synclock = new Object();
		timers = new TimerThread[MConfig.max_timer_tasks];
		procs = new MProcess[MConfig.max_timer_tasks];

		for (int i=0; i<MConfig.max_timer_tasks; i++) {
			timers[i] = null;
			procs[i] = null;
		}
		return;
	}
	//}}}
	//{{{  public static void add_to_timer_queue (MProcess p, long millis)
	/**
	 * called to add a process to the timer queue
	 *
	 * @param p process to add
	 * @param millis timeout in milli-seconds
	 */
	public static void add_to_timer_queue (MProcess p, long millis)
	{
		int idx;

		synchronized (synclock) {
			/* already waiting ? */
			for (idx=0; (idx<procs.length) && (procs[idx] != p); idx++);
			if (idx < procs.length) {
				/* already waiting, so cancel the old one */
				procs[idx] = null;
			}
			/* find a free slot */
			for (idx=0; (idx<timers.length) && ((timers[idx] != null) || (procs[idx] != null)); idx++);
			if (idx == procs.length) {
				/* no free slots! */
				MKernel.panic ("MTimer::add_to_timer_queue() no free timer slots");
			}
			/* stick in slot and start timer process */
			procs[idx] = p;
			timers[idx] = new TimerThread (idx, millis);
			timers[idx].start ();
		}
	}
	//}}}
	//{{{  public static boolean del_from_timer_queue (MProcess p)
	/**
	 * called to remove a process from the timer queue
	 *
	 * @param p process to remove
	 *
	 * @return true if the timeout happened, false otherwise
	 */
	public static boolean del_from_timer_queue (MProcess p)
	{
		int idx;
		boolean removed = false;

		synchronized (synclock) {
			/* remove all instances */
			for (idx=0; idx<procs.length; idx++) {
				if (procs[idx] == p) {
					procs[idx] = null;
					removed = true;
				}
			}
		}
		return removed;
	}
	//}}}
}


