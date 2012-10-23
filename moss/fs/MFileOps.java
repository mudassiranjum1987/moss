/*
 *	MFileOps.java -- interface for things that we can treat like a file
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
 * This interface defines kernel "file-operations".  Besides its obvious use for "real" file-handling,
 * it is used to interface any stream in general, including pipes, character devices and block devices.
 *
 * Character devices treat read/write in the way that would be expected, lseek() is probably meaningless though.
 *
 * Block devices treat read/write/lseek a bit differently:
 *      read() must be called with the correct sized block of data (ie, the device block-size)
 *      write() must be used in similar manner
 *      lseek() offsets are <em>block offsets</em>, not byte-offsets.  BEG/CUR/END are still valid, however
 * How a block-device handles its own internal management of the "offset" is up to it..  i'd suggest
 * all operations sharing a common offset, that is the block offset, which read() and write() increment.
 */

public interface MFileOps
{
	/**
	 * used to open the file.  This is only ever called once for each file-handle when
	 * the file is opened for the first time.
	 *
	 * @param handle file-handle of file being opened
	 * @param flags open flags (for devices, minor number is <tt>(flags &gt;&gt; 16)</tt>)
	 *
	 * @return 0 on success or &lt; 0 indicating error
	 */
	public int open (MFile handle, int flags);

	/**
	 * used to close the file.  This is only ever called once for each file-handle.
	 *
	 * @param handle file-handle of file being closed
	 *
	 * @return 0 on success or &lt; 0 indicating error
	 */
	public int close (MFile handle);

	/**
	 * seek to a specific offset
	 *
	 * @param handle file-handle
	 * @param offset byte-offset relative to "whence"  (block offset for block devices)
	 * @param whence constant indicating position seek should occur from
	 *
	 * @return new absolute offset or &lt; 0 indicating error
	 */
	public int lseek (MFile handle, int offset, int whence);

	/**
	 * read bytes from file
	 *
	 * @param handle file-handle
	 * @param buffer buffer where data will be stored
	 * @param count maximum number of bytes to read
	 *
	 * @return number of bytes read, 0 on end-of-file or &lt; 0 indicating error
	 */
	public int read (MFile handle, byte buffer[], int count);

	/**
	 * write bytes to file
	 *
	 * @param handle file-handle
	 * @param buffer data to write
	 * @param count maximum number of bytes to write
	 *
	 * @return number of bytes written or &lt; 0 indicating error
	 */
	public int write (MFile handle, byte buffer[], int count);

	/**
	 * file-handle control
	 *
	 * @param handle file-handle
	 * @param op operation to perform
	 * @param arg argument to operation (if applicable)
	 *
	 * @return &gt;= 0 on success, or &lt; 0 indicating failure
	 */
	public int fcntl (MFile handle, int op, int arg);

	
	/** get file-descriptor flags */
	public static final int F_GETFL = 1;
	/** set file-descriptor flags */
	public static final int F_SETFL = 2;


	/** non-blocking flag */
	public static final int O_NONBLOCK = 0x0001;
	/** close on exec flag */
	public static final int O_CLOEXEC = 0x0002;

	 
	/** open file for reading */
	public static final int OPEN_READ = 0x0001;
	/** open file for writing */
	public static final int OPEN_WRITE = 0x0002;
	/** create file if it doesn't exist */
	public static final int OPEN_CREAT = 0x0004;
	/** truncate the file (empty it) if it exists */
	public static final int OPEN_TRUNC = 0x0008;


	/** seek from beginning of file */
	public static final int LSEEK_BEG = 0;
	/** seek from current offset */
	public static final int LSEEK_CUR = 1;
	/** seek from end of file */
	public static final int LSEEK_END = 2;


	/** file exists */
	public static final int F_OK = 0x0001;
	/** file is readable */
	public static final int R_OK = 0x0002;
	/** file is writable */
	public static final int W_OK = 0x0004;
	/** file is executable */
	public static final int X_OK = 0x0008;

}

