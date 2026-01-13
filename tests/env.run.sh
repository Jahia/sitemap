#!/bin/bash
version=$(node -p "require('./package.json').devDependencies['@jahia/cypress']")
echo "HERE"
echo Using @jahia/cypress@$version...
npx --yes --package @jahia/cypress@$version env.run
