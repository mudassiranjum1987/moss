/*
 *	UMailSend.java -- mail-box test sender
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

import moss.user.*;

/*
 * Note: this is invoked from the first test program (UMailRecv)
 */

public class UMailSend implements MUserProcess
{
	public int main (String argv[], MEnv envp)
	{
		int x, other;
		
		/* say hello and get PID of other process from arguments */
		MPosixIf.writestring (MPosixIf.STDOUT, argv[0] + " -- mail-box sender process\n");

		if (argv.length < 2) {
			MPosixIf.writestring (MPosixIf.STDOUT, argv[0] + ": um, incorrect usage!\n");
			MPosixIf.exit (1);
		}
		try {
			other = Integer.parseInt (argv[1]);
		} catch (NumberFormatException e) {
			MPosixIf.writestring (MPosixIf.STDOUT, argv[0] + ": bad argument [" + argv[1] + "]\n");
			MPosixIf.exit (1);
			other = -1;
		}

		/* send 4 messages */
		MPosixIf.writestring (MPosixIf.STDOUT, argv[0] + ": sending sleepy messages to PID " + other + "...\n");
		
		for (int i=0; i<4; i++) {
			String msg = new String ("Hello, mailbox world " + i + " :)");

			if ((i % 2) == 0) {
				MPosixIf.sleep (2000);
			}
			MPosixIf.sendmsg (other, 41+i, (Object)msg);
		}

		MPosixIf.writestring (MPosixIf.STDOUT, argv[0] + ": done.  exiting.\n");
		return 0;
	}

	public void signal (int signo, Object sigdata)
	{
		MPosixIf.writestring (MPosixIf.STDOUT, "UPipeTest2 signalled with " + signo + "!\n");
	}

}

