/*
 *	UKill.java -- kill process
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

public class UKill implements MUserProcess
{
	public int main (String argv[], MEnv envp)
	{
		int signo, i;
		int pid;
		
		if (argv.length < 2) {
			MPosixIf.writestring (MPosixIf.STDERR, argv[0] + ": missing argument, --help for help.\n");
			MPosixIf.exit (1);
		}
		for (i = 1; i < argv.length; i++) {
			if (argv[i].equals ("--help") || argv[i].equals ("-h")) {
				String help[] = {"Usage: " + argv[0] + " [signal] <pid>\n",
						"where [signal] is either a signal name or number\n",
						"default signal sent is TERM (15)\n"};

				for (int j = 0; j < help.length; j++) {
					MPosixIf.writestring (MPosixIf.STDOUT, help[j]);
				}
				MPosixIf.exit (0);
			} else if (argv[i].charAt(0) == '-') {
				MPosixIf.writestring (MPosixIf.STDERR, argv[0] + ": unrecognised option: " + argv[i] + "\n");
				MPosixIf.exit (1);
			} else {
				break;		/* for() */
			}
		}
		if (i == argv.length) {
			MPosixIf.writestring (MPosixIf.STDERR, argv[0] + ": <pid> argument required.\n");
			MPosixIf.exit (1);
		}

		if (i == (argv.length - 1)) {
			signo = MSignal.SIGTERM;
		} else {
			signo = -1;

			/* try number conversion first */
			try {
				signo = Integer.parseInt (argv[i]);
			} catch (NumberFormatException e) {
				signo = -1;
			}

			if (signo < 0) {
				/* match argument i against signal names */
				String signameends[] = new String[] {"", "HUP", "INT", "QUIT",
								"ILL", "", "", "",
								"PFE", "KILL", "USR1", "SEGV",
								"USR2", "PIPE", "ALRM", "TERM",
								"", "CHLD", "CONT", "STOP"};
				int j;

				for (j=0; j<signameends.length; j++) {
					if (signameends[j].equals (argv[i])) {
						signo = j;
						break;		/* for () */
					}
				}
			}
			if (signo < 0) {
				MPosixIf.writestring (MPosixIf.STDOUT, argv[0] + ": bad signal [" + argv[i] + "]\n");
				return 1;
			}
			i++;
		}

		/* oki, parse PID */
		try {
			pid = Integer.parseInt (argv[i]);
		} catch (NumberFormatException e) {
			MPosixIf.writestring (MPosixIf.STDOUT, argv[0] + ": bad PID [" + argv[i] + "]\n");
			return 1;
		}

		if (signo >= MSignal.SIG_NSIGS) {
			MPosixIf.writestring (MPosixIf.STDOUT, argv[0] + ": bad signal (" + signo + ")\n");
			return 1;
		}
			

		/* okay, try and kill process */
		i = MPosixIf.kill (pid, signo);
		if (i < 0) {
			MPosixIf.writestring (MPosixIf.STDOUT, argv[0] + ": failed to kill process: " +
					MStdLib.strerror (i) + "\n");
			return 1;
		}
		return 0;
	}

	public void signal (int signo, Object sigdata)
	{
		MPosixIf.writestring (MPosixIf.STDERR, "UKill signalled with " + signo + "!\n");
	}

}

