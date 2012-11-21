/*
 *	UConsole.java -- simple console process, acts as a sort of command-line interface
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

import java.util.*;
import moss.user.*;
import moss.fs.MFileOps;

/**
 * This class provides a basic console interface to MOSS
 */

public class UConsole implements MUserProcess
{
	//{{{  private stuff
	/** process name */
	private String pname = null;
	/** process environment */
	private MEnv envp = null;

	//{{{  public class JobInfo
	/**
	 * this class holds "jobs" information
	 */
	public class JobInfo
	{
		public static final int JOB_FOREGROUND = 0;
		public static final int JOB_BACKGROUND = 1;
		public static final int JOB_STOPPED = 2;
		public static final int JOB_FINISHED = 3;

		public String cmd;		/* command */
		public String args[];		/* arguments (including 0th) */
		public int pid;
		public int status;
		public int scode;		/* for finished jobs */
	}
	//}}}
	
	/** list of jobs */
	private JobInfo jobs[] = null;
	/** number of jobs in "finished" state */
	private int finjobs = 0;


	//{{{  public class Alias
	/**
	 * this class deals with aliases/hashes for commands
	 */
	public class Alias
	{
		public String name;		/* short name */
		public String cmd;		/* full path */
		public String args[];		/* arguments (including 0th) */
	}
	//}}}
	
	/** aliases based on hashed name */
	private Hashtable aliases = null;
	/** list of aliases */
	private ArrayList aaliases = null;

	//}}}

	//{{{  public void signal (int signo, Object sigdata)
	/**
	 * signal handler
	 *
	 * @param signo signal number
	 * @param sigdata signal-specific data
	 */
	public void signal (int signo, Object sigdata)
	{
		int i;

		switch (signo) {
		case MSignal.SIGCHLD:
			//{{{  should be a known child process..!
			{
				int sdata[] = (int[])sigdata;

				if (jobs == null) {
					break;			/* switch() */
				}
				for (i=0; i<jobs.length; i++) {
					if ((jobs[i] != null) && (jobs[i].pid == sdata[0])) {
						/* this one finished */
						if (jobs[i].status == JobInfo.JOB_FINISHED) {
							/* ho hum */
							MPosixIf.writestring (MPosixIf.STDERR, pname + ": pid " + sdata[0] + " alreay terminated (status " + sdata[1] + ")\n");
						} else {
							jobs[i].status = JobInfo.JOB_FINISHED;
							jobs[i].scode = sdata[1];
							finjobs++;
						}
						break;
					}
				}
				if (i == jobs.length) {
					MPosixIf.writestring (MPosixIf.STDERR, pname + ": received SIGCHLD for pid " + sdata[0] + " (status " + sdata[1] + ")\n");
				}
			}
			break;
			//}}}
		default:
			MPosixIf.writestring (MPosixIf.STDOUT, "UConsole::signal() with " + signo + "!\n");
			break;
		}
	}
	//}}}

	//{{{  private int dosourcefile (String fname)
	/**
	 * sources a given file -- reads lines of commands from it
	 *
	 * @param fname filename to source
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	private int dosourcefile (String fname)
	{
		int fd;
		byte buffer[] = null;

		fd = MPosixIf.open (fname, MFileOps.OPEN_READ);
		if (fd < 0) {
			return fd;
		}

		/* read file contents */
		for (boolean done = false; !done;) {
			int v, left;
			byte tmpbuf[];

			if (buffer != null) {
				left = 1024 - buffer.length;
			} else {
				left = 1024;
			}
			tmpbuf = new byte[left];

			v = MPosixIf.read (fd, tmpbuf, left);
			if (v < 0) {
				MPosixIf.close (fd);
				return v;
			} else if (v == 0) {
				/* EOF */
				done = true;
			} else {
				/* add new stuff to buffer */
				if (buffer == null) {
					buffer = tmpbuf;
				} else {
					byte t2buf[] = new byte[buffer.length + tmpbuf.length];

					System.arraycopy (buffer, 0, t2buf, 0, buffer.length);
					System.arraycopy (tmpbuf, 0, t2buf, buffer.length, tmpbuf.length);
					buffer = t2buf;
				}
			}

			/* scan for lines */
			v = 0;
			while (v < ((buffer == null) ? 0 : buffer.length)) {
				for (v = 0; (v < buffer.length) && (buffer[v] != '\n'); v++);
				if (v < buffer.length) {
					/* got one! */
					String cmd = new String (buffer, 0, v);
					byte t2buf[];

					process_command (cmd);
					/* shuffle up */
					if (v == (buffer.length - 1)) {
						buffer = null;
					} else {
						t2buf = new byte[buffer.length - (v + 1)];

						System.arraycopy (buffer, v+1, t2buf, 0, t2buf.length);
						buffer = t2buf;
					}
				}
			}

			/* go round again for more */
		}

		MPosixIf.close (fd);
		return 0;
	}
	//}}}
	//{{{  public int main (String argv[], MEnv envp)
	/**
	 * process entry-point.
	 *
	 * @param argv arguments
	 * @param envp environment
	 *
	 * @return exit-code (0 on success, 1 on failure)
	 */
	public int main (String argv[], MEnv envp)
	{
		int maxcmdlen = 128;
		byte buf[] = new byte[maxcmdlen];
		
		this.envp  = envp;
		this.pname = argv[0];
		this.jobs = null;
		this.finjobs = 0;
		this.aliases = new Hashtable ();
		this.aaliases = new ArrayList ();

		MPosixIf.signal (MSignal.SIGCHLD, MSignal.SIG_CATCH);

		MPosixIf.writestring (MPosixIf.STDOUT, "UConsole (" + pname + ") starting\n");
		/* source /etc/console if it exists */
		if (MPosixIf.access ("/etc/console", MFileOps.R_OK) == 0) {
			dosourcefile ("/etc/console");
		}

		while (true) {
			int v;

			/* read user input */
			while (true) {
				/* collect any finished jobs */
				if (finjobs > 0) {
					for (v=0; (v<jobs.length) && (finjobs > 0); v++) {
						if ((jobs[v] != null) && (jobs[v].status == JobInfo.JOB_FINISHED)) {
							MPosixIf.writestring (MPosixIf.STDOUT, "[" + v + "] done " + jobs[v].cmd + " (pid " + jobs[v].pid + ") status " + jobs[v].scode + "\n");
							finjobs--;
							jobs[v] = null;
						}
					}
				}
				MPosixIf.writestring (MPosixIf.STDOUT, "MOSS# ");
				v = MPosixIf.read (MPosixIf.STDIN, buf, maxcmdlen);
				if (v != -MSystem.EINTR) {
					break;
				} else {
					MPosixIf.writestring (MPosixIf.STDOUT, "interrupted system call!\n");
				}
			}
			if (v < 0) {
				MPosixIf.writestring (MPosixIf.STDOUT, "\nconsole read error (" + (-v) + "), exiting\n");
				MPosixIf.exit (0);
			} else if (v == 0) {
				MPosixIf.writestring (MPosixIf.STDOUT, "\nEOF on stdin, exiting\n");
				MPosixIf.exit (0);
			}
			/* if we get this far, something was read successfully */
			
			/* break at newlines and process */
			for (int start = 0, newstart = -1; start < v; start = newstart) {
				int stop;

				for (stop = start; (stop < v) && (buf[stop] != '\n'); stop++);
				newstart = stop + 1;
				stop--;		/* index of last character in whatever */
				/* trim leading and trailing whitespace */
				for (; (start <= stop) && ((buf[start] == ' ') || (buf[start] == '\t')); start++);
				for (; (stop >= start) && ((buf[stop] == ' ') || (buf[stop] == '\t')); stop--);
				if (stop <= start) {
					/* blank line */
				} else {
					String cmd = new String (buf, start, (stop - start) + 1);

					process_command (cmd);
				}
			}
		}
	}
	//}}}
	//{{{  private void process_command (String cmd)
	/**
	 * processes the given command
	 * 
	 * @param cmd command string
	 */
	private void process_command (String cmd)
	{
		String bits[];
		int nbits = 0;
		cmd = cmd.replaceAll("[\\r\\n]", "");		//Remove carriage return that has been added by <manj>
		int clen = cmd.length();

		if (clen == 0) {
			return;
		} else {
				int i;

			for (i=0; (i<clen) && ((cmd.charAt(i) == ' ') || (cmd.charAt(i) == '\t')); i++);
			if (i == clen) {
				return;
			} else if (cmd.charAt(i) == '#') {
				return;
			}
		}
		bits = MStdLib.split_string (cmd);
		nbits = bits.length;
		if (nbits == 0) {
			MPosixIf.writestring (MPosixIf.STDOUT, "invalid command [" + cmd + "]\n");
			return;
		}

		/* now we do a crude test.. */
		if (bits[0].equals ("getppid")) {
			//{{{  getppid
			int ppid = MPosixIf.getppid ();

			MPosixIf.writestring (MPosixIf.STDOUT, "parent process PID is " + ppid + "\n");
			//}}}
		} else if (bits[0].equals ("getpid")) {
			//{{{  getpid
			int pid = MPosixIf.getpid ();

			MPosixIf.writestring (MPosixIf.STDOUT, "this process PID is " + pid + "\n");
			//}}}
		} else if (bits[0].equals ("printenv")) {
			//{{{  printenv
			int i;

			MPosixIf.writestring (MPosixIf.STDOUT, "current environment:\n");
			for (i=0; i<envp.env.length; i++) {
				MPosixIf.writestring (MPosixIf.STDOUT, "    " + envp.env[i] + "\n");
			}
			//}}}
		} else if (bits[0].equals ("setenv")) {
			//{{{  setenv
			if (nbits < 3) {
				MPosixIf.writestring (MPosixIf.STDOUT, "setenv requires name and value arguments\n");
			} else {
				MStdLib.setenv (envp, bits[1], bits[2]);
			}
			//}}}
		} else if (bits[0].equals ("exit")) {
			//{{{  exit
			int i;

			if (jobs != null) {
				for (i=0; i<jobs.length; i++) {
					if ((jobs[i] != null) && (jobs[i].status != JobInfo.JOB_FINISHED)) {
						break;		/* for() */
					}
				}
				if (i != jobs.length) {
					MPosixIf.writestring (MPosixIf.STDOUT, pname + ": there are unfinished jobs.\n");
				} else {
					MPosixIf.exit (0);
				}
			} else {
				MPosixIf.exit (0);
			}
			//}}}
		} else if (bits[0].equals ("jobs")) {
			//{{{  jobs
			int i;

			if (jobs != null) {
				for (i=0; i<jobs.length; i++) {
					if (jobs[i] != null) {
						MPosixIf.writestring (MPosixIf.STDOUT, "[" + i + "] " + jobs[i].cmd + " (pid " + jobs[i].pid + ")\n");
					}
				}
			}
			//}}}
		} else if (bits[0].equals ("alias")) {
			//{{{  alias
			if (bits.length == 1) {
				MPosixIf.writestring (MPosixIf.STDOUT, pname + ": \"alias\" requires an argument\n");
			} else if ((bits.length == 2) && (bits[1].equals ("-l"))) {
				/* list aliases */
				int i;

				for (i=0; i<aaliases.size(); i++) {
					Alias ali = (Alias)(aaliases.get (i));
					String r = ali.name + ": " + ali.cmd;
					int j;

					for (j=1; j<ali.args.length; j++) {
						r = r + " " + ali.args[j];
					}
					r = r + "\n";
					MPosixIf.writestring (MPosixIf.STDOUT, r);
				}
			} else if ((bits.length == 3) && (bits[1].equals ("-d"))) {
				/* delete alias */
				Alias ali = (Alias)(aliases.get (bits[2]));

				if (ali == null) {
					MPosixIf.writestring (MPosixIf.STDOUT, "no such alias: " + bits[2] + "\n");
				} else {
					int i = aaliases.indexOf (ali);

					aliases.remove (ali.name);
					aaliases.remove (i);
				}
			} else if (bits.length == 2) {
				/* alias query */
				Alias ali = (Alias)(aliases.get (bits[1]));

				if (ali == null) {
					MPosixIf.writestring (MPosixIf.STDOUT, "no such alias: " + bits[1] + "\n");
				} else {
					int i = aaliases.indexOf (ali);
					String r = ali.name + ": " + ali.cmd;
					int j;

					for (j=1; j<ali.args.length; j++) {
						r = r + " " + ali.args[j];
					}
					r = r + "\n";
					MPosixIf.writestring (MPosixIf.STDOUT, r);
				}
			} else {
				/* creating a new alias */
				if (bits[2].charAt (0) != '/') {
					MPosixIf.writestring (MPosixIf.STDOUT, "alias must be to a full path.\n");
				} else if (aaliases.indexOf (bits[1]) >= 0) {
					MPosixIf.writestring (MPosixIf.STDOUT, "alias " + bits[1] + " already exists (delete it first)\n");
				} else {
					Alias ali = new Alias ();

					ali.name = bits[1];
					ali.cmd = bits[2];
					ali.args = new String[bits.length - 2];
					System.arraycopy (bits, 2, ali.args, 0, ali.args.length);

					/* add it */
					aliases.put (ali.name, ali);
					aaliases.add (ali);
				}
			}
			//}}}
		} else if (bits[0].equals ("help")) {
			//{{{  display some help
			String help[] = new String[] {
				"help                         displays this help\n",
				"getpid                       get this process ID\n",
				"getppid                      get parent process ID\n",
				"printenv                     print the current environment\n",
				"setenv <name> <value>        adds/modifies an entry in the current environment\n",
				"alias <cmd> <path> [args]    create alias\n",
				"alias <cmd>                  show alias\n",
				"alias -l                     list aliases\n",
				"alias -d <cmd>               delete alias\n",
				"jobs                         show jobs\n",
				"<cmd> [args] &               run command in background\n",
				"<cmd> [args]                 run command in foreground\n",
				"exit                         terminate this console process\n"};

			for (int i=0; i<help.length; i++) {
				MPosixIf.writestring (MPosixIf.STDOUT, help[i]);
			}
			//}}}
		} else {
			//{{{  running a program (maybe)
			String progname = bits[0];
			Alias ali = (Alias)aliases.get (progname);
			boolean bground = false;

			/* see if there's a "&" on the end (background) */
			if (bits[bits.length - 1].equals ("&")) {
				String newbits[] = new String[bits.length - 1];

				System.arraycopy (bits, 0, newbits, 0, bits.length - 1);
				bits = newbits;
				bground = true;
			}

			if (ali != null) {
				/* handle plain aliases specially */
				if (ali.args.length == 1) {
					progname = ali.cmd;
				} else {
					/* copy arguments over */
					String newbits[] = new String[bits.length + ali.args.length - 1];

					progname = ali.cmd;
					System.arraycopy (bits, 1, newbits, ali.args.length, bits.length - 1);
					System.arraycopy (ali.args, 1, newbits, 1, ali.args.length - 1);
					newbits[0] = bits[0];
					bits = newbits;
				}
			}

			if (progname.charAt(0) == '/') {
				int pid;

				/* might be a file to run -- try it.. */
				pid = MPosixIf.forkexec (progname, bits);
				if (pid < 0) {
					MPosixIf.writestring (MPosixIf.STDOUT, "failed to execute: " + MStdLib.strerror (pid) + "\n");
				} else {
					JobInfo job = new JobInfo ();
					int i;

					job.cmd = progname;
					job.args = bits;
					job.pid = pid;
					job.status = bground ? JobInfo.JOB_BACKGROUND : JobInfo.JOB_FOREGROUND;
					job.scode = -1;

					if (jobs == null) {
						jobs = new JobInfo[1];

						jobs[0] = job;
						i = 0;
					} else {
						for (i=0; (i<jobs.length) && (jobs[i] != null); i++);
						if (i == jobs.length) {
							JobInfo newjobs[] = new JobInfo[jobs.length + 1];

							System.arraycopy (jobs, 0, newjobs, 0, jobs.length);
							jobs = newjobs;
						}
						jobs[i] = job;
					}

					if (!bground) {
						boolean gotit = (jobs[i].status == JobInfo.JOB_FINISHED);

						/* loop until we get signalled saying it's done */
						while (!gotit) {
							MPosixIf.pause ();
							gotit = (jobs[i].status == JobInfo.JOB_FINISHED);
						}

						/* then trash it (stops console reporting as if bg) */
						jobs[i] = null;
					}
				}
			} else {
				MPosixIf.writestring (MPosixIf.STDOUT, "unrecognised command: " + bits[0] + "\n");
			}
			//}}}
		}
	}
	//}}}
}

