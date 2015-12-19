#!/bin/bash

java -Xmx1024m -jar lib/${project.artifactId}-${project.version}.jar $*
