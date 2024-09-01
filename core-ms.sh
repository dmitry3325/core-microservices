#!/bin/bash

gitBaseUrl="https://gitlab.com/Dmitry3325/"
mainBranch="main"

curDir=$(pwd)

cloneAll() {
  repos=(
    "common"
    "notification-ms"
  )

  for repo in ${repos[@]}; do
    msUrl="$gitBaseUrl$repo.git"
    echo "Cloning $repo repository"
    git clone "$msUrl"
  done
}

switchToMain() {
  for d in $curDir/*/; do
    echo "Trying to switch to main $(basename $d)"
    cd "$d";
    if [ ! -d $d/.git ]; then
      cd ..
      continue
    fi

    echo "Switching $(basename $d) to the main $mainBranch"
    git checkout "$mainBranch"
  done
  cd "$curDir";
}

pullAll() {
  for d in $curDir/*/; do
    cd "$d";
    if [ ! -d $d/.git ]; then
      cd ..
      continue
    fi

    echo "Pulling $d"
    branch=$(git branch | sed -nr 's/\*\s(.*)/\1/p')
    if [ -z $branch ] || [ $branch != "$mainBranch" ]; then
      echo "!!!!!!!!!!!!!!!!!!!! WARNING !!!!!!!!!!!!!!!!!!!!"
      echo "$(basename $d) in not at master but at $branch"
      echo "!!!!!!!!!!!!!!!!!!!! WARNING !!!!!!!!!!!!!!!!!!!!"
    fi
    git pull
    cd ..;
  done
  cd "$curDir";
}

deleteMergedBranches() {
  for d in $curDir/*/; do
    cd "$d";
    if [ ! -d $d/.git ]; then
      cd ..
      continue
    fi

    echo "Pruning locally untracked branches from $(basename $d)"
    branches=$(git branch --merged "$mainBranch" | grep -v "^[ *]*$mainBranch")
    if [[ $branches ]]; then
      git branch --merged "$mainBranch" | grep -v "^[ *]*$mainBranch$" | xargs git branch -d;
    fi
    cd ..;
  done
  cd "$curDir";
}

"$@"
