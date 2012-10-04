/*
 *	MLog.java -- kernel log
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

import moss.fs.*;
import moss.drivers.*;
import moss.user.*;
import moss.ipc.*;


public class MLog
{
	//{{{  private stuff
	/** where the messages are stored */
	private static String[] log;
	/** log line-pointer */
	private static int lines;
	/** named-message queue handle for logging */
	private static MFile default_log;

	//{{{  private static class PFS_log implements MProcFSIf
	/**
	 * this class provides the contents of /proc/klog
	 */
	private static class PFS_log implements MProcFSIf
	{
		public String readproc (MInode inode, String name)
		{
			String r = "";
			int i;

			/* build up the log backwards */
			for (i=((lines + log.length - 1) % log.length); i != lines; i = ((i + log.length - 1) % log.length)) {
				if (log[i] != null) {
					r += log[i] + "\n";
				}
			}
			return r;
		}
	}
	//}}}
	//}}}
	
	//{{{  public static void init_log ()
	/**
	 * called to initialise the log handler to its default state
	 */
	public static void init_log ()
	{
		log = new String[MConfig.kernel_log_lines];
		lines = 0;
		default_log = null;
	}
	//}}}
	//{{{  public static void finalinit ()
	/**
	 * called to perform final initialisations on the log
	 */
	public static void finalinit ()
	{
		default_log = new MFile ();
		default_log.refcount = 1;
		default_log.pdata = null;
		default_log.offset = 0;
		default_log.fileif = null;
		default_log.dirif = null;
		
		if (MNamedMsgQ.nmq_open (default_log, "klog") < 0) {
			default_log = null;
		}

		/* provide an entry in /proc/ that can read the kernel-log */
		MProcFS.register_procimpl ("klog", new PFS_log());

		return;
	}
	//}}}
	//{{{  public static void writelog (String str)
	/**
	 * writes a line to the kernel log
	 *
	 * @param str string to write
	 */
	public static void writelog (String str)
	{
		log[lines] = str;
		lines = (lines + 1) % MConfig.kernel_log_lines;

		System.err.println (str);

		if (default_log == null) {
			/* without this we just generate on MOSS's stderr */
			System.err.println (str);
		} else {
			int r;

			r = MNamedMsgQ.nmq_send (default_log, 0, (Object)str);
			if (r < 0) {
				default_log.fileif.close (default_log);
				default_log = null;
				writelog ("error writing to kernel log queue");
			}
		}
		return;
	}
	//}}}
}


