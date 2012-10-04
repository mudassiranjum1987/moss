/*
 *	MFSOps.java -- file-system operations for MOSS
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
 * This interface define kernel "file-system operations".  File-system drivers must
 * implement this (probably in addition to MFileOps and MDirOps).
 */

public interface MFSOps
{
	/**
	 * this is called when a file-system is first created and mounted.  This should
	 * be used to provide any file-system specific setup.
	 *
	 * @param mount_path full path from the root (/) to where this is being mounted.  A single
	 * 				"/" indicates that this is being mounted as the root file-system.
	 * @param options file-system specific options
	 *
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public int mount (String mount_path, String options[]);

	/**
	 * this is called before a file-system is un-mounted.
	 *
	 * @param options file-system specific un-mounting options
	 *
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public int umount (String options[]);

	/**
	 * called to open a file.
	 *
	 * @param path relative path to the file (e.g. `etc/passwd' for the root file-system)
	 * @param handle file-handle to be associated with this file
	 * @param flags open flags
	 * @param mode file mode if creating
	 *
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public int open (String path, MFile handle, int flags, int mode);

	/**
	 * called to open a directory (read-only!).
	 *
	 * @param path relative path to the directory (e.g. `/etc/init.d' for the root file-system)
	 * @param handle file-handle to be associated with this directory
	 *
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public int opendir (String path, MFile handle);

	/**
	 * creates a directory in the file-system
	 *
	 * @param path relative path to the directory to be created
	 * @param flags flags (mode)
	 *
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public int mkdir (String path, int flags);

	/**
	 * removes a name from the file-system (may be a directory -- must be empty)
	 *
	 * @param path relative path to the name to be removed
	 *
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public int unlink (String path);

	/**
	 * removes a directory from the file-system (must be empty)
	 *
	 * @param path relative path to the directory to be removed
	 *
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public int rmdir (String path);

	/**
	 * creates a `hard-link'
	 *
	 * @param oldpath existing relative path (e.g. local/packages/nmh for "usr" file-system)
	 * @param newpath new relative path
	 *
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public int link (String oldpath, String newpath);

	/**
	 * creates a symbolic link
	 *
	 * @param oldpath full path to existing name (e.g. /usr/local/packages/nmh-1.4 for "usr" file-system)
	 * @param newpath relative path to new link (e.g. local/packages/nmh for "usr" file-system)
	 *
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public int symlink (String oldpath, String newpath);

	/**
	 * creates a special file on the file-system
	 *
	 * @param path relative path to file to create
	 * @param mode file mode flags
	 * @param majmin major and minor numbers `<tt>((major &lt;&lt; 16) | minor)</tt>' for block/char special files,
	 * 			otherwise ignored.
	 *
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public int mknod (String path, int mode, int majmin);

	/**
	 * reads the contents of a symbolic-link on the file-system
	 *
	 * @param path relative path to symbolic link on file-system
	 * @param buf buffer where the link data is stored
	 * @param buflen maximum number of bytes to write into `buf'
	 *
	 * @return number of bytes read on success, or &lt; 0 indicating error
	 */
	public int readlink (String path, byte[] buf, int buflen);

	/**
	 * sets access and modification times for a file (inode)
	 *
	 * @param path relative path to name
	 * @param times array of two `<tt>long</tt>'s that are the access and modification times respectively
	 *
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public int utime (String path, long[] times);

	/**
	 * retrieves information about a name on the file-system
	 *
	 * @param path relative path to name
	 * @param statbuf buffer in which information is placed
	 *
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public int stat (String path, MInode statbuf);

	/**
	 * tests for access to a file
	 *
	 * @param path relative path to name
	 * @param amode access mode ([FRWX]_OK constants)
	 *
	 * @return 0 on success, or &lt; 0 indicating error
	 */
	public int access (String path, int amode);
}


