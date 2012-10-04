/*
 *	MConfig.java -- system constants (assorted)
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

/**
 * This class defines various constants used throughout the moss kernel.
 */

public class MConfig
{
	//{{{  virtual hardware
	/** number of virtual processors */
	public static final int ncpus = 2;


	//}}}
	//{{{  general things
	/** simple version string */
	public static final String version = "0.2.3";
	/** long version string */
	public static final String long_version = "MOSS/0.2.3";


	//}}}
	//{{{  various system size constants
	/** maximum number of open files an MProcess may have */
	public static final int max_files_per_process = 32;
	/** pipe (IPC) buffer size */
	public static final int pipe_buffer_size = 4096;
	/** pipe (IPC) buffer mask */
	public static final int pipe_buffer_mask = 0x0fff;
	/** maximum number of concurrent timers */
	public static final int max_timer_tasks = 128;
	/** maximum number of device drivers (major device numbers/names) */
	public static final int max_device_drivers = 256;
	/** maximum number of mounted file-systems */
	public static final int max_mounted_fs = 32;
	/** number of lines in the kernel log-buffer */
	public static final int kernel_log_lines = 64;

	//}}}
	//{{{  some major device numbers
	/** console */
	public static final int DEV_MAJOR_CONSOLE = 0;
	/** ramdisk */
	public static final int DEV_MAJOR_RAMDISK = 129;
	//}}}

}

