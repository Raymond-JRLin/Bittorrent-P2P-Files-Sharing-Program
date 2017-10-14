# Bittorrent-P2P-Files-Sharing-Program

This project is going to design a peer-to-peer network for files uploading and downloading with some features of Bit-torrent.

It requires to chop files to several chunks with 100Kb to distribute them to all clients and integratedly rebuild the original files. So this program constructs multi-thread sharing servers and clients to share chunks at the same time, the basic idea is to arrange all clients as a circle, every client receive chunks from previous client and share to next one. It Implementes lossless transmissions for clients to chop files, connect, share and integrate data with each other.
