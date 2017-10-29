Configuration
=============

The configuration of the @PLUGIN@ plugin is done in the `gerrit.config`
file.

```
  [plugin "@PLUGIN@"]
    url = http://phabricator.example.com
    changeUrl = http://example.org/account.html
    sizeParameter = s=${size}x${size}
    token = cli-aaaaaaaaa
```

<a id="url">
`plugin.@PLUGIN@.url`
:	The url for the phab install. For example http://phabricator.example.com

<a id="token">
`plugin.@PLUGIN@.token`
:	The token to authenticate over the conduit.

<a id="changeUrl">
`plugin.@PLUGIN@.changeUrl`
:	The URL shown in Gerrit's user settings to tell the user, where the
	avatar can be changed. The placeholder `${user}` will
	be replaced by the `username` and `${email}` will be replaced with
	the user's `email address`. Optional.

<a id="sizeParameter">
`plugin.@PLUGIN@.sizeParameter`
:	URL parameter with `${size}` placeholder to forward the preferred
	image size to the avatar provider. Optional.

Please note that `http://` URLs will be automatically rewritten to
`https://`, if `gerrit.canonicalWebUrl` uses HTTPS.