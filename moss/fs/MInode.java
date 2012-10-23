/*
 *	MInode.java -- inode data-type
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

package moss.fs;

/**
 * this class is used to represent an "inode".  It is used mainly when
 * dealing with file-system objects -- MFile contains a reference to this.
 */

public class MInode
{
	/** major device number */
	public int major = -1;
	/** minor device number */
	public int minor = -1;
	/** inode number (device dependent) */
	public int ino = -1;
	/** protection */
	public int mode = 0;
	/** number of hard links */
	public int nlinks = 0;
	/** user-ID of owner */
	public int uid = 0;
	/** group-ID of owner */
	public int gid = 0;
	/** total size, in bytes */
	public long size = 0;
	/** block-size (for block device I/O) */
	public int blksize = 0;
	/** number of blocks allocated */
	public long nblocks = 0;
	/** time of last access */
	public long atime = 0;
	/** time of last modification (writes, etc.) */
	public long mtime = 0;
	/** time of last inode change (perm, owner, size, etc.) */
	public long ctime = 0;

	//{{{  mode constants
	/** bitmask for file-type bitfields */
	public static final int S_IFMT = 0170000;
	/** socket */
	public static final int S_IFSOCK = 0140000;
	/** symbolic link */
	public static final int S_IFLNK = 0120000;
	/** regular file */
	public static final int S_IFREG = 0100000;
	/** block device */
	public static final int S_IFBLK = 0060000;
	/** directory */
	public static final int S_IFDIR = 0040000;
	/** character device */
	public static final int S_IFCHR = 0020000;
	/** fifo */
	public static final int S_IFIFO = 0010000;
	/** mask for file permissions */
	public static final int S_IMPERM = 0007777;
	/** set UID bit */
	public static final int S_ISUID = 0004000;
	/** set GID bit */
	public static final int S_ISGID = 0002000;
	/** sticky-bit */
	public static final int S_ISVTX = 0001000;
	//}}}
}


