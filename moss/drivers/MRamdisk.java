/*
 *	MRamdisk.java -- ramdisk block device for MOSS
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

package moss.drivers;

import moss.kernel.*;
import moss.fs.*;
import moss.user.*;

/**
 * this implements a "ramdisk" block device
 */
public class MRamdisk implements MFileOps
{
	//{{{  private vars
	/** the ramdisk */
	private byte disk[][];
	/** a handy blank-block */
	private byte blank_block[];

	/* this is a half-meg ramdisk */
	/** ramdisk size (blocks) */
	private static final int nblocks = 256;
	/** ramdisk block-size (bytes) */
	private static final int blksize = 2048;

	/** current block offset */
	private int offset;
	//}}}
	
	//{{{  public MRamdisk ()
	/**
	 * constructor -- initialises the ramdisk
	 */
	public MRamdisk ()
	{
		disk = new byte[nblocks][];
		offset = 0;
		blank_block = new byte[blksize];
		for (int i=0; i<blksize; i++) {
			blank_block[i] = 0x00;
		}
		MDevices.register_driver ("ramdisk", MConfig.DEV_MAJOR_RAMDISK, this);

		/* register with the device file-system */
		MDevFS.register_device ("ramdisk", MConfig.DEV_MAJOR_RAMDISK, 0, MInode.S_IFBLK | 0666);
	}
	//}}}
	//{{{  public int open (MFile handle, int flags)
	/**
	 * called when opening the ramdisk;  stores reference to self in the file-handle
	 *
	 * @param handle file-handle
	 * @param flags open flags (ignored)
	 *
	 * @return 0 on success or &lt; 0 on error
	 */
	public int open (MFile handle, int flags)
	{
		handle.pdata = (Object)this;
		return 0;
	}
	//}}}  
	//{{{  public int close (MFile handle)
	/**
	 * called when closing the ramdisk (it remains intact, however, unless garbage-collected)
	 *
	 * @param handle file-handle
	 *
	 * @return 0 on success or &lt; 0 on error
	 */
	public int close (MFile handle)
	{
		handle.pdata = null;
		return 0;
	}
	//}}}  
	//{{{  public int lseek (MFile handle, int offset, int whence)
	/**
	 * seeks to a specific block in the ramdisk.  Seeking to the "end-of-file" is allowed (not much use, though)
	 *
	 * @param handle file-handle
	 * @param offset block offset
	 * @param whence where to seek from (LSEEK_{BEG,CUR,END})
	 *
	 * @return new absolute block offset, or &lt; 0 indicating error
	 */
	public int lseek (MFile handle, int offset, int whence)
	{
		synchronized (this) {
			switch (whence) {
			case MFileOps.LSEEK_CUR:
				offset = this.offset + offset;
				break;
			case MFileOps.LSEEK_END:
				offset = nblocks - offset;
				break;
			}
			if ((offset < 0) || (offset > nblocks)) {
				return -MSystem.EINVAL;
			}
			this.offset = offset;
		}
		return offset;
	}
	//}}}  
	//{{{  public int read (MFile handle, byte buffer[], int count)
	/**
	 * called to read a block from the ramdisk (at the current offset)
	 *
	 * @param handle file-handle
	 * @param buffer buffer where data will be stored
	 * @param count maximum number of bytes to read
	 *
	 * @return number of bytes read, 0 on end-of-file, or &lt; 0 on error
	 */
	public int read (MFile handle, byte buffer[], int count)
	{
		if ((count != blksize) || (buffer == null) || (buffer.length != blksize)) {
			return -MSystem.EINVAL;
		}
		synchronized (this)
		{
			if (offset == nblocks) {
				return 0;
			} else if (disk[offset] == null) {
				System.arraycopy (blank_block, 0, buffer, 0, blksize);
				offset++;
			} else {
				System.arraycopy (disk[offset], 0, buffer, 0, blksize);
				offset++;
			}
		}
		return blksize;
	}
	//}}}  
	//{{{  public int write (MFile handle, byte buffer[], int count)
	/**
	 * called to write data to the ramdisk (at the current offset)
	 *
	 * @param handle file handle
	 * @param buffer data to write
	 * @param count number of bytes to write
	 *
	 * @return number of bytes written or &lt; 0 on error
	 */
	public int write (MFile handle, byte buffer[], int count)
	{
		if ((count != blksize) || (buffer == null) || (buffer.length != blksize)) {
			return -MSystem.EINVAL;
		}
		synchronized (this)
		{
			if (offset == nblocks) {
				return -MSystem.ENOSPC;
			} else {
				disk[offset] = (byte[])(buffer.clone());
				offset++;
			}
		}
		return blksize;
	}
	//}}}  
	//{{{  public int fcntl (MFile handle, int op, int arg)
	public int fcntl (MFile handle, int op, int arg)
	{
		return -MSystem.EIO;
	}
	//}}}
}


