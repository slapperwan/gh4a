<img alt="Octodroid" align="right" src="https://raw.githubusercontent.com/slapperwan/gh4a/master/app/src/main/res/drawable-xxhdpi/octodroid.png">

OctoDroid
=========
This application provides access to [GitHub](https://github.com/) and lets you stay connected with your network

Download
--------
<a href='https://play.google.com/store/apps/details?id=com.gh4a'><img alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png' height="80px" align="left"/></a>[<img src="https://f-droid.org/badge/get-it-on.png" alt="Get it on F-Droid" height="80px">](https://f-droid.org/packages/com.gh4a/)

Main features
-------------

### Repository
* List repositories
* Watch/unwatch repository
* View branches/tags
* View pull requests
* View contributors
* View watchers/networks
* View issues

### User
* View basic information
* Activity feeds
* Follow/unfollow user
* View public/watched repositories
* View followers/following
* View organizations (if type is user)
* View members (if type is organization)

### Issue
* List issues
* Filter by label, assignee or milestone
* Create/edit/close/reopen issue
* Comment on issue
* Manage labels
* Manage milestones

### Commit
* View commit (shows files changed/added/deleted)
* Diff viewer with colorized HTML
* View commit history on each file

### Tree/File browser
* Browse source code
* View code with syntax highlighting

### Gist
* List public gists
* View gist content

### Explore Github
* Public timeline
* Trending repos (today, week, month, forever)
* GitHub blog

*..and many more*

How to Build Octodroid
----------------------
- Ensure Android SDK platform and build-tools are installed
- Register an application for your OctoDroid usage under your [GitHub settings](https://github.com/settings/developers)
  * naming is up to you
  * callback URL must be gh4a://oauth
- Create a client.properties file with the following content:
```
ClientId="<CLIENT ID DISPLAYED IN APPLICATION SETTINGS>"
ClientSecret="<CLIENT SECRET DISPLAYED IN APPLICATION SETTINGS>"
```

- Build using Gradle

```bash
./gradlew assembleDebug
```

- To get a full list of available tasks

```bash
./gradlew tasks
```

Open Source Libraries
---------------------
* [android-gif-drawable](https://github.com/koral--/android-gif-drawable)
* [AndroidSVG](https://github.com/BigBadaboom/androidsvg)
* [AndroidX](https://github.com/androidx/androidx)
* [emoji-java](https://github.com/vdurmont/emoji-java)
* [GitHubSdk](https://github.com/maniac103/GitHubSdk)
* [HoloColorPicker](https://github.com/LarsWerkman/HoloColorPicker)
* [MarkdownEdit](https://github.com/Tunous/MarkdownEdit)
* [Material Design Icons](https://github.com/google/material-design-icons)
* [PrettyTime](https://github.com/ocpsoft/prettytime)
* [Recycler Fast Scroll](https://github.com/pluscubed/recycler-fast-scroll)
* [Retrofit](https://github.com/square/retrofit)
* [RxAndroid](https://github.com/ReactiveX/RxAndroid)
* [RxJava](https://github.com/ReactiveX/RxJava)
* [RxLoader](https://github.com/maniac103/RxLoader)
* [SmoothProgressBar](https://github.com/castorflex/SmoothProgressBar)

Contributions
-------------
* [kageiit](https://github.com/kageiit) - Improvements and bug fixes
* [maniac103](https://github.com/maniac103) - Improvements, bug fixes and new features
* [ARoiD](https://github.com/ARoiD) - Testing
* [extremis (Steven Mautone)](https://github.com/extremis) - OctoDroid name and the new icon
* [zquestz](https://github.com/zquestz) - Thanks for the application icon
* [cketti](https://github.com/cketti)
* [Tunous](https://github.com/Tunous) - Improvements, bug fixes and new features
