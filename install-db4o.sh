#!/bin/sh
VERSION=7.12.121
BUILD=14025
LIB=dev/db4o-7.12/lib
mvn install:install-file -Dfile=$LIB/bloat-1.0.jar -DgroupId=com.db4o -DartifactId=db4o-bloat -Dversion=$VERSION.$BUILD -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=$LIB/db4o-$VERSION.$BUILD-all-java1.1.jar -DgroupId=com.db4o -DartifactId=db4o-all-java1.1 -Dversion=$VERSION.$BUILD -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=$LIB/db4o-$VERSION.$BUILD-all-java1.2.jar -DgroupId=com.db4o -DartifactId=db4o-all-java1.2 -Dversion=$VERSION.$BUILD -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=$LIB/db4o-$VERSION.$BUILD-all-java5.jar -DgroupId=com.db4o -DartifactId=db4o-all-java5 -Dversion=$VERSION.$BUILD -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=$LIB/db4o-$VERSION.$BUILD-bench.jar -DgroupId=com.db4o -DartifactId=db4o-bench -Dversion=$VERSION.$BUILD -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=$LIB/db4o-$VERSION.$BUILD-core-java1.1.jar -DgroupId=com.db4o -DartifactId=db4o-core-java1.1 -Dversion=$VERSION.$BUILD -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=$LIB/db4o-$VERSION.$BUILD-core-java1.2.jar -DgroupId=com.db4o -DartifactId=db4o-core-java1.2 -Dversion=$VERSION.$BUILD -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=$LIB/db4o-$VERSION.$BUILD-bench.jar -DgroupId=com.db4o -DartifactId=db4o-bench -Dversion=$VERSION.$BUILD -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=$LIB/db4o-$VERSION.$BUILD-core-java5.jar -DgroupId=com.db4o -DartifactId=db4o-core-java5 -Dversion=$VERSION.$BUILD -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=$LIB/db4o-$VERSION.$BUILD-cs-java1.1.jar -DgroupId=com.db4o -DartifactId=db4o-cs-java1.1 -Dversion=$VERSION.$BUILD -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=$LIB/db4o-$VERSION.$BUILD-cs-java1.2.jar -DgroupId=com.db4o -DartifactId=db4o-cs-java1.2 -Dversion=$VERSION.$BUILD -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=$LIB/db4o-$VERSION.$BUILD-cs-java5.jar -DgroupId=com.db4o -DartifactId=db4o-cs-java5 -Dversion=$VERSION.$BUILD -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=$LIB/db4o-$VERSION.$BUILD-db4ounit-java1.1.jar -DgroupId=com.db4o -DartifactId=db4o-db4ounit-java1.1 -Dversion=$VERSION.$BUILD -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=$LIB/db4o-$VERSION.$BUILD-db4ounit-java1.2.jar -DgroupId=com.db4o -DartifactId=db4o-db4ounit-java1.2 -Dversion=$VERSION.$BUILD -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=$LIB/db4o-$VERSION.$BUILD-db4ounit-java5.jar -DgroupId=com.db4o -DartifactId=db4o-db4ounit-java5 -Dversion=$VERSION.$BUILD -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=$LIB/db4o-$VERSION.$BUILD-instrumentation-java1.2.jar -DgroupId=com.db4o -DartifactId=db4o-instrumentation-java1.2 -Dversion=$VERSION.$BUILD -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=$LIB/db4o-$VERSION.$BUILD-instrumentation-java5.jar -DgroupId=com.db4o -DartifactId=db4o-instrumentation-java5 -Dversion=$VERSION.$BUILD -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=$LIB/db4o-$VERSION.$BUILD-nqopt-java1.2.jar -DgroupId=com.db4o -DartifactId=db4o-nqopt-java1.2 -Dversion=$VERSION.$BUILD -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=$LIB/db4o-$VERSION.$BUILD-nqopt-java5.jar -DgroupId=com.db4o -DartifactId=db4o-nqopt-java5 -Dversion=$VERSION.$BUILD -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=$LIB/db4o-$VERSION.$BUILD-optional-java1.1.jar -DgroupId=com.db4o -DartifactId=db4o-optional-java1.1 -Dversion=$VERSION.$BUILD -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=$LIB/db4o-$VERSION.$BUILD-optional-java1.2.jar -DgroupId=com.db4o -DartifactId=db4o-optional-java1.2 -Dversion=$VERSION.$BUILD -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=$LIB/db4o-$VERSION.$BUILD-optional-java5.jar -DgroupId=com.db4o -DartifactId=db4o-optional-java5 -Dversion=$VERSION.$BUILD -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=$LIB/db4o-$VERSION.$BUILD-osgi.jar -DgroupId=com.db4o -DartifactId=db4o-osgi -Dversion=$VERSION.$BUILD -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=$LIB/db4o-$VERSION.$BUILD-osgi-test.jar -DgroupId=com.db4o -DartifactId=db4o-osgi-test -Dversion=$VERSION.$BUILD -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=$LIB/db4o-$VERSION.$BUILD-taj-java1.2.jar -DgroupId=com.db4o -DartifactId=db4o-taj-java1.2 -Dversion=$VERSION.$BUILD -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=$LIB/db4o-$VERSION.$BUILD-taj-java5.jar -DgroupId=com.db4o -DartifactId=db4o-taj-java5 -Dversion=$VERSION.$BUILD -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=$LIB/db4o-$VERSION.$BUILD-tools-java1.2.jar -DgroupId=com.db4o -DartifactId=db4o-tools-java1.2 -Dversion=$VERSION.$BUILD -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=$LIB/db4o-$VERSION.$BUILD-tools-java5.jar -DgroupId=com.db4o -DartifactId=db4o-tools-java5 -Dversion=$VERSION.$BUILD -Dpackaging=jar -DgeneratePom=true
