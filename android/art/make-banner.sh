#!/bin/sh
# Gera os banners do Google TV (640x360) a partir de banner.html, com o Chrome em modo headless:
# drawable-nodpi/banner.png em inglês (por omissão) e drawable-pt-nodpi/banner.png em português.
set -e
cd "$(dirname "$0")"
res=../app/src/main/res
shot() {
  mkdir -p "$res/$1"
  "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" \
    --headless --disable-gpu --hide-scrollbars --allow-file-access-from-files \
    --force-device-scale-factor=2 --window-size=320,180 --virtual-time-budget=2000 \
    --screenshot="$PWD/$res/$1/banner.png" \
    "file://$PWD/banner.html$2"
}
shot drawable-nodpi ""
shot drawable-pt-nodpi "?pt"
