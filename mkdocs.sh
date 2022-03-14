#!/bin/bash

# Fail on any error
set -ex

# Copy over stuff into docs/ to prevent duplication
cp README.md docs/index.md
cp LICENSE docs/license.md

# Convert absolute links to relative links in the docs/index.md
sed -i '' 's/\/docs/\./g' docs/index.md

# Fix reference to license
sed -i '' 's/\(LICENSE\)/license\.md/g' docs/index.md

# View website locally
# mkdocs serve

# Generate website
mkdocs build