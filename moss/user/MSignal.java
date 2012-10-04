/*
 *	MSignal.java -- signal class for MOSS
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

package moss.user;

/**
 * this class defines a "signal" -- that can be delivered to a user-process.
 */

public class MSignal {
	//{{{  signal constants
	/** hang-up, controlling terminal/process died */
	public static final int SIGHUP = 1;
	/** interrupted */
	public static final int SIGINT = 2;
	/** quit */
	public static final int SIGQUIT = 3;
	/** illegal instruction */
	public static final int SIGILL = 4;
	/** floating-point exception */
	public static final int SIGFPE = 8;
	/** kill signal (non-catchable) */
	public static final int SIGKILL = 9;
	/** segmentation fault (invalid memory reference) */
	public static final int SIGSEGV = 11;
	/** broken pipe (write to closed pipe) */
	public static final int SIGPIPE = 13;
	/** alarm signal (timer alarm) */
	public static final int SIGALRM = 14;
	/** regular kill signal (catchable) */
	public static final int SIGTERM = 15;
	/** user-defined signal 1 */
	public static final int SIGUSR1 = 10;
	/** user-defined signal 2 */
	public static final int SIGUSR2 = 12;
	/** child-process stopped or terminated */
	public static final int SIGCHLD = 17;
	/** continue stopped process (job control) */
	public static final int SIGCONT = 18;
	/** stop process */
	public static final int SIGSTOP = 19;

	/** number of signals */
	public static final int SIG_NSIGS = 32;
	//}}}
	//{{{  signal-handling constants (this varies slightly from POSIX)
	/** default action */
	public static final int SIG_DFL = 0;
	/** ignore signal */
	public static final int SIG_IGN = 1;
	/** catch signal (deliver to process) */
	public static final int SIG_CATCH = 2;
	//}}}


	//{{{  public vars
	/** which signal this one is */
	public int signo;
	/** additional data */
	public Object sigdata;
	/** next signal in list */
	public MSignal next;
	//}}}
	//{{{  public MSignal (int sig, Object data)
	/**
	 * constructor, creates a new signal
	 *
	 * @param sig signal number
	 * @param data any additional data to be sent
	 */
	public MSignal (int sig, Object data)
	{
		signo = sig;
		sigdata = data;
		next = null;
	}
	//}}}

}

