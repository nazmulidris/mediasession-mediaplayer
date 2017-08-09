Simple MediaPlayer Sample
=========================

This sample demonstrates the use of `MediaSession` with `MediaPlayer`.
Here are some important things this sample highlights:

This project is a copy of the 
[Playing music on cars and wearables codelab](https://codelabs.developers.google.com/codelabs/android-music-player/).

The code has been modified to:
- Use a different MediaPlayer wrapper (MediaPlayerHolder.java) that does
  not do audio focus.
- Have a super simple UI (no list view).

More information on MediaSession:
- [YouTube video on MediaSessionCompat](https://youtu.be/FBC1FgWe5X4).
- [Medium Article on MediaBrowserServiceCompat](https://medium.com/google-developers/mediabrowserservicecompat-and-the-modern-media-playback-app-7959a5196d90).

Screenshots
===========

![](screenshots/screenshots.png "Playback UI")

License
-------

Copyright 2017 Nazmul Idris. All rights reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.