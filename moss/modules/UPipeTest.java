/*
 *	UPipeTest.java -- pipe-testing program
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


public class UPipeTest implements MUserProcess
{
	private String argv[];

	private boolean gotpipesig;

	public int main (String argv[], MEnv envp)
	{
		int fds[] = new int[2];
		int x;
		byte buffer[];
		
		/* set environment */
		this.argv = argv;

		/* say hello and create pipe */
		MPosixIf.writestring (MPosixIf.STDOUT, argv[0] + " -- pipe test..\n");
		x = MPosixIf.pipe (fds);
		if (x < 0) {
			MPosixIf.writestring (MPosixIf.STDOUT, argv[0] + ": pipe creation failed: " + MStdLib.strerror(x) + "\n");
			MPosixIf.exit (1);
		}

		/* start process that will read from the pipe */
		x = MPosixIf.forkexec ("/bin/pipetest2", new String[] {"/bin/pipetest2", "" + fds[0]});
		if (x < 0) {
			MPosixIf.writestring (MPosixIf.STDOUT, argv[0] + ": failed to execute /bin/pipetest2: " + MStdLib.strerror(x) + "\n");
			MPosixIf.exit (1);
		}
		MPosixIf.writestring (MPosixIf.STDOUT, argv[0] + ": started pipe reader, pid " + x + "\n");

		/* we don't want the reading end anymore */
		MPosixIf.close (fds[0]);

		/* write something to the pipe */
		x = MPosixIf.writestring (fds[1], "Hello world, down the pipe!");
		if (x < 0) {
			MPosixIf.writestring (MPosixIf.STDOUT, argv[0] + ": write error to pipe: " + MStdLib.strerror(x) + "\n");
			MPosixIf.exit (1);
		}
		MPosixIf.writestring (MPosixIf.STDOUT, argv[0] + ": wrote " + x + " bytes.  close()ing the pipe..\n");
		x = MPosixIf.close (fds[1]);
		if (x < 0) {
			MPosixIf.writestring (MPosixIf.STDOUT, argv[0] + ": error closing the pipe: " + MStdLib.strerror(x) + "\n");
			MPosixIf.exit (1);
		}

		/* reschedule */
		MPosixIf.reschedule ();

		/* create a new pipe and test local SIGPIPE generation */
		MPosixIf.writestring (MPosixIf.STDOUT, argv[0] + ": testing local SIGPIPE generation..\n");
		x = MPosixIf.pipe (fds);
		if (x < 0) {
			MPosixIf.writestring (MPosixIf.STDOUT, argv[0] + ": pipe creation failed: " + MStdLib.strerror(x) + "\n");
			MPosixIf.exit (1);
		}

		gotpipesig = false;

		/* set SIGPIPE handling */
		MPosixIf.signal (MSignal.SIGPIPE, MSignal.SIG_CATCH);

		/* close reading-end of the pipe and write something */
		MPosixIf.close (fds[0]);
		x = MPosixIf.writestring (fds[1], "Hello world, down the pipe!");

		/* if it worked, the signal will have been delivered already */
		if (gotpipesig) {
			MPosixIf.writestring (MPosixIf.STDOUT, argv[0] + ": got SIGPIPE, write() returned: " + MStdLib.strerror(x) + "\n");
		} else {
			MPosixIf.writestring (MPosixIf.STDOUT, argv[0] + ": didn\'t get SIGPIPE, write() returned: " + MStdLib.strerror(x) + ".  Exiting..\n");
			MPosixIf.exit (1);
		}

		/* close it and reset signal handling */
		MPosixIf.close (fds[1]);
		MPosixIf.signal (MSignal.SIGPIPE, MSignal.SIG_DFL);

		/* create a new pipe and test local end-of-file generation */
		MPosixIf.writestring (MPosixIf.STDOUT, argv[0] + ": testing local end-of-file generation..\n");
		x = MPosixIf.pipe (fds);
		if (x < 0) {
			MPosixIf.writestring (MPosixIf.STDOUT, argv[0] + ": pipe creation failed: " + MStdLib.strerror(x) + "\n");
			MPosixIf.exit (1);
		}

		/* close writing-end and attempt read */
		MPosixIf.close (fds[1]);
		buffer = new byte[128];
		x = MPosixIf.read (fds[0], buffer, buffer.length);

		MPosixIf.writestring (MPosixIf.STDOUT, argv[0] + ": read() returned " + x + ": " + ((x > 0) ? "[...]" : MStdLib.strerror(x)) + "\n");
		MPosixIf.close (fds[0]);

		MPosixIf.writestring (MPosixIf.STDOUT, argv[0] + ": all done here :)\n");
		return 0;
	}

	public void signal (int signo, Object sigdata)
	{
		MPosixIf.writestring (MPosixIf.STDOUT, argv[0] + ": signalled with " + signo + "!\n");
		if (signo == MSignal.SIGPIPE) {
			gotpipesig = true;
		}
		return;
	}

}

