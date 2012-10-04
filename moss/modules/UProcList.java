/*
 *	UProcList.java -- show processes utility for MOSS
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
import moss.kernel.MProcess;	/* need process structure */


public class UProcList implements MUserProcess
{
	private String pad_to (String str, int len)
	{
		if (str.length() >= len) {
			return str;
		}
		while ((str.length() + 5) < len) {
			str = str + "     ";
		}
		while (str.length() < len) {
			str = str + " ";
		}
		return str;
	}

	public int main (String argv[], MEnv envp)
	{
		MProcess procdata[];
		
		procdata = MPosixIf.process_list ();
		if (procdata == null) {
			MPosixIf.writestring (MPosixIf.STDOUT, argv[0] + ": failed to get process list\n");
			MPosixIf.exit (1);
		}
		/*                                  --> 1                 19   24   29    35              51             66  */
		MPosixIf.writestring (MPosixIf.STDOUT, "name              pid  ppid state syscall         aruments\n");
		MPosixIf.writestring (MPosixIf.STDOUT, "----------------------------------------------------------------------\n");
		for (int i=0; i<procdata.length; i++) {
			String str = "";

			/* this isn't wholly pleasant.. */
			str = str + procdata[i].getName();
			str = pad_to (str, 19);
			str = str + procdata[i].pid;
			str = pad_to (str, 24);
			str = str + procdata[i].ppid;
			str = pad_to (str, 29);
			switch (procdata[i].state) {
			case MProcess.TASK_INVALID:	str = str + "-";	break;
			case MProcess.TASK_STOPPED:	str = str + "T";	break;
			case MProcess.TASK_RUNNABLE:	str = str + "Q";	break;
			case MProcess.TASK_RUNNING:	str = str + "R";	break;
			case MProcess.TASK_SLEEPING:	str = str + "S";	break;
			case MProcess.TASK_FINISHED:	str = str + "F";	break;
			case MProcess.TASK_ZOMBIE:	str = str + "Z";	break;
			default:
				str = str + "?";
				break;
			}
			if (procdata[i].signalled) {
				str = str + "*";
			}
			str = pad_to (str, 35);
			if (procdata[i].syscall == null) {
				str = str + "-";
			} else {
				str = str + procdata[i].syscall;
			}
			str = pad_to (str, 51);
			for (int j=0; j<procdata[i].cmdline.length; j++) {
				str = str + procdata[i].cmdline[j] + " ";
			}
			if (str.length() > 78) {
				str = str.substring (0, 78);
			}
			str = str + "\n";
			/* and then we can write it.. :) */
			MPosixIf.writestring (MPosixIf.STDOUT, str);
		}
		return 0;
	}

	public void signal (int signo, Object sigdata)
	{
		MPosixIf.writestring (MPosixIf.STDOUT, "UProcList signalled with " + signo + "!\n");
	}

}

