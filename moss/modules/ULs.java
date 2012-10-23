/*
 *	ULs.java -- list directory utility
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
import moss.fs.MDirEnt;
import moss.fs.MInode;

public class ULs implements MUserProcess
{
	public int main (String argv[], MEnv envp)
	{
		int fd;
		boolean longlist = false;
		String path = null;

		if (argv.length < 2) {
			MPosixIf.writestring (MPosixIf.STDERR, argv[0] + ": missing argument, --help for help.\n");
			MPosixIf.exit (1);
		}
		for (int i = 1; i < argv.length; i++) {
			if (argv[i].equals ("--help") || argv[i].equals ("-h")) {
				String help[] = {"Usage: " + argv[0] + " [options] <path>\n",
						"where [options] are:\n",
						"    -l             generates a long listing\n",
						"    -h | --help    shows this help\n"};

				for (int j = 0; j < help.length; j++) {
					MPosixIf.writestring (MPosixIf.STDOUT, help[j]);
				}
				MPosixIf.exit (0);
			} else if (argv[i].equals ("-l")) {
				longlist = true;
			} else if (argv[i].charAt(0) == '-') {
				MPosixIf.writestring (MPosixIf.STDERR, argv[0] + ": unrecognised option: " + argv[i] + "\n");
				MPosixIf.exit (1);
			} else if (path == null) {
				path = argv[i];
			} else {
				MPosixIf.writestring (MPosixIf.STDERR, argv[0] + ": only one <path> supported, using: " + path + "\n");
			}
		}
		if (path == null) {
			MPosixIf.writestring (MPosixIf.STDERR, argv[0] + ": <path> required.\n");
			MPosixIf.exit (1);
		}
		fd = MPosixIf.opendir (path);
		if (fd < 0) {
			MPosixIf.writestring (MPosixIf.STDERR, argv[0] + ": failed to opendir(): " + MStdLib.strerror (fd) + "\n");
			MPosixIf.exit (0);
		} else {
			String permbits = "xwrxwrxwr";		/* backwards.. */
			MDirEnt dirent = new MDirEnt ();
			int code;

			MPosixIf.writestring (MPosixIf.STDOUT, "listing of " + path + ":\n");

			code = MPosixIf.readdir (fd, dirent);
			while (code == 0) {
				if (!longlist) {
					MPosixIf.writestring (MPosixIf.STDOUT, dirent.d_name + "\n");
				} else {
					int mode = (dirent.d_mode & MInode.S_IMPERM);
					char pchars[] = new char[10];
					int statcode;
					MInode statbuf = new MInode ();
					String fullpath, outstr;

					if (path.charAt (path.length() - 1) != '/') {
						fullpath = path + "/" + dirent.d_name;
					} else {
						fullpath = path + dirent.d_name;
					}
					statcode = MPosixIf.stat (fullpath, statbuf);

					if (statcode < 0) {
						MPosixIf.writestring (MPosixIf.STDERR, "\n" + argv[0] + ": failed to stat " + fullpath + ": " + MStdLib.strerror (statcode) + "\n");
						MPosixIf.exit (1);
					}

					for (int i=0; i<pchars.length; i++) {
						pchars[i] = '-';
					}

					if ((dirent.d_mode & MInode.S_IFMT) == MInode.S_IFDIR) {
						pchars[0] = 'd';
					}
					/* fill in defaults */
					for (int i=8; i >= 0; i--) {
						if (((mode >> i) & 1) == 1) {
							pchars[9 - i] = permbits.charAt(i);
						}
					}
					if ((mode & MInode.S_ISVTX) != 0) {
						pchars[9] = 't';
					}
					if ((mode & MInode.S_ISUID) != 0) {
						pchars[3] = 's';
					}
					if ((mode & MInode.S_ISGID) != 0) {
						pchars[6] = 's';
					}

					outstr = MStdLib.sprintf ("%s %-8d %-5d %-25s\n", new Object[] {new String(pchars), new Integer((int)statbuf.size), new Integer(dirent.d_ino), dirent.d_name});

					MPosixIf.writestring (MPosixIf.STDOUT, outstr);
				}
				code = MPosixIf.readdir (fd, dirent);
			}
			MPosixIf.close (fd);
		}
		return 0;
	}

	public void signal (int signo, Object sigdata)
	{
		MPosixIf.writestring (MPosixIf.STDERR, "ULs signalled with " + signo + "!\n");
	}

}

