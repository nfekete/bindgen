#! /bin/python
# -*- coding: utf-8 -*-

from __future__ import unicode_literals
from __future__ import print_function

from datetime import datetime
import os
import random
import re
import shlex
import sys
import tempfile

try:  # py3
  from shlex import quote
except ImportError:  # py2
  from pipes import quote

# where the config is written
configuration_file = "gitlab-ci.build.conf"
# base listen port; this base port is incremented
# to obtain each needed port
# tempdir
temp_dir = tempfile.mkdtemp(dir=os.getcwd(), suffix="-bindgen")
# built branch
branch = os.environ.get("CI_COMMIT_REF_NAME", None)
maven_opts = shlex.split(os.environ.get("MAVEN_OPTS", ""))

# check if branch is available
if not branch:
  print("Fatal error; current branch information not available")
  sys.exit(1)

# check if branch needs a sonar custom setting
main_branches = ["dev", "master"]
if len([ i for i in main_branches if re.match(i, branch) ]) == 0:
  maven_opts.append("-Dsonar.projectBranch={branch}".format(branch=branch))

# location of test classes
maven_opts.append("-Djava.io.tmpdir={}".format(temp_dir))

conf = """
# generated on {date}

export MAVEN_OPTS={maven_opts}

mkdir -p {temp_dir}

""".format(
  date=datetime.now().isoformat(),
  maven_opts=quote(' '.join([i for i in maven_opts])),
  temp_dir=quote(temp_dir)
).strip()

# write and display config
with open(configuration_file, "w") as f:
  f.write(conf)

print("Generated configuration file")
print("============================")
print(conf)
print("============================")

sys.exit(0)
