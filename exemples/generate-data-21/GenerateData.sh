#!/usr/bin/bash

get_absolute_path() {
    case "$1" in
        /*) path="$1";;
        *)  path="$(pwd)/$1";;
    esac
    realpath "$path"
}

script_name=$(basename "$0")
app_name=${script_name/sh/java}
bin_dir=$(get_absolute_path "$(dirname "$0")")
app_dir=$(dirname "${bin_dir}")

APP_DIR=${app_dir} "${app_dir}/scripts/${app_name}" "$@"
