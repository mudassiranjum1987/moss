/*
 *	KLogSvr.java -- kernel log server process for MOSS
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

package moss.modules;

import moss.kernel.*;
import moss.fs.*;
import moss.ipc.*;
import moss.user.*;

public class KLogSvr implements MKernelProcess
{
	//{{{  public int main (String argv[])
	/**
	 * entry-point for the kernel module
	 *
	 * @param argv module arguments (name and any options)
	 */
	public int main (String argv[])
	{
		int x;
		int msgfd;

		/* open the message queue */
		msgfd = MPosixIf.opennmq ("klog");
		if (msgfd < 0) {
			System.err.println (argv[0] + ": failed to create named message queue \"klog\": " +
					MStdLib.strerror (msgfd));
			return 1;
		}

		System.err.println (argv[0] + ": starting..");

		for (;;) {
			int i;
			MNamedMsgQ.MNamedMsg msg = new MNamedMsgQ.MNamedMsg ();

			i = MPosixIf.readmsg (msgfd, msg);
			if (i < 0) {
				System.err.println (argv[0] + ": failed to read from kernel log: " +
						MStdLib.strerror (i));
			} else {
				String s = (String)(msg.msg);

				System.err.println ("KLOG: " + s);
			}
		}
		// return 0;
	}
	//}}}
}


