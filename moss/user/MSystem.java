/*
 *	MSystem.java -- various visible constants
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

public class MSystem {
	//{{{  error constants, from Linux/POSIX
	/** operation successful */
	public static final int ESUCCESS = 0;
	/** operation not permitted */
	public static final int EPERM = 1;
	/** no such file or directory */
	public static final int ENOENT = 2;
	/** no such process */
	public static final int ESRCH = 3;
	/** interrupted system call */
	public static final int EINTR = 4;
	/** I/O error */
	public static final int EIO = 5;
	/** no such device or address */
	public static final int ENXIO = 6;
	/** argument list too long */
	public static final int E2BIG = 7;
	/** exec format error */
	public static final int ENOEXEC = 8;
	/** bad file number */
	public static final int EBADF = 9;
	/** no child processes */
	public static final int ECHILD = 10;
	/** try again */
	public static final int EAGAIN = 11;
	/** out of memory */
	public static final int ENOMEM = 12;
	/** permission denied */
	public static final int EACCESS = 13;
	/** bad address */
	public static final int EFAULT = 14;
	/** block device required */
	public static final int EBLKDEV = 15;
	/** device or resource busy */
	public static final int EBUSY = 16;
	/** file already exists */
	public static final int EEXISTS = 17;
	/** cross-device link */
	public static final int EXDEV = 18;
	/** no such device */
	public static final int ENODEV = 19;
	/** not a directory */
	public static final int ENOTDIR = 20;
	/** is a directory */
	public static final int EISDIR = 21;
	/** invalid argument */
	public static final int EINVAL = 22;
	/** file table overflow */
	public static final int ENFILE = 23;
	/** too many open files */
	public static final int EMFILE = 24;
	/** not a typewriter */
	public static final int ENOTTY = 25;
	/** text file busy */
	public static final int ETXTBSY = 26;
	/** file too large */
	public static final int EFBIG = 27;
	/** no space left on device */
	public static final int ENOSPC = 28;
	/** illegal seek */
	public static final int ESPIPE = 29;
	/** read-only file-system */
	public static final int EROFS = 30;
	/** too many links */
	public static final int EMLINK = 31;
	/** broken pipe */
	public static final int EPIPE = 32;

	/** file-name too long */
	public static final int ENAMETOOLONG = 36;
	/** function not implemented */
	public static final int ENOSYS = 38;

	//}}}
}


