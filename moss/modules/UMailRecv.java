/*
 *	UMailRecv.java -- mail-box test receiver
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


public class UMailRecv implements MUserProcess
{
	public int main (String argv[], MEnv envp)
	{
		int fds[] = new int[2];
		int x;
		
		/* say hello */
		MPosixIf.writestring (MPosixIf.STDOUT, argv[0] + " -- mail-box receiver process\n");

		/* start process that will send us a message */
		x = MPosixIf.forkexecc ("/bin/mailsend", new String[] {"/bin/mailsend", "" + MPosixIf.getpid()});
		if (x < 0) {
			MPosixIf.writestring (MPosixIf.STDOUT, argv[0] + ": failed to execute /bin/mailsend: " + MStdLib.strerror (x) + "\n");
			MPosixIf.exit (1);
		}
		MPosixIf.writestring (MPosixIf.STDOUT, argv[0] + ": started message sender, pid " + x + "\n");

		/* oki, receive 4 messages: (any process, any type), (any process, specific type), (specific process, any type), (specific process, specific type) */
		int pid_args[] =  new int[] {-1, -1, x,  x};
		int type_args[] = new int[] {-1, 42, -1, 44};
		int retry = 3;

		for (int i = 0; i < pid_args.length; i++) {
			String msg = (String)MPosixIf.recvmsg (pid_args[i], type_args[i]);

			if (msg == null) {
				MPosixIf.writestring (MPosixIf.STDOUT, argv[0] + ": MPosixIf.recvmsg() returned null! -- retry..\n");
				if (retry == 0) {
					MPosixIf.writestring (MPosixIf.STDOUT, argv[0] + ": giving up..\n");
					retry = 3;
				} else {
					retry--;
					i--;
				}
			} else {
				MPosixIf.writestring (MPosixIf.STDOUT, argv[0] + ": got message [" + msg + "]\n");
			}
		}

		return 0;
	}

	public void signal (int signo, Object sigdata)
	{
		MPosixIf.writestring (MPosixIf.STDOUT, "UMailRecv signalled with " + signo + "!\n");
	}

}

