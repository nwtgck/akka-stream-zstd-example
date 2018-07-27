#! /bin/sh

cd `dirname $0`

# (from: http://sun.aei.polsl.pl/~sdeor/index.php?page=silesia)
curl http://sun.aei.polsl.pl/~sdeor/corpus/dickens.bz2 | bzip2 -d > dickens
