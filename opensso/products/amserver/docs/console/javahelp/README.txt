$Id: README.txt,v 1.1 2006/05/04 06:54:32 veiming Exp $

AM 7.0 Online Help Guidelines

* The (default) fallback locale "en" must exist.

* Files that need to be localized:

  index.xml
  toc.xml
  all html files

* Files that doesn't need to be localized:

  app.hs
  map.jhm

* Assumptions

  - Locale "en" always exists.
  - Files app.hs and map.jhm in all locales have the exact same contents.
  - The target names (as defined in map.jhm) follow naming conventions of
    ViewBeans.  For example, target "realm.RMRealm" maps to
    com.sun.identity.console.realm.RMRealmViewBean class.

* Document Rules

  - All *.hs, *.jhm, *.xml files are wellformed and valid.
  - In toc.xml, every single <tocitem> should have a "target" attribute with
    a non-empty value.
  - In toc.xml, the value of every tocitem's target attribute should have a
    mapID entry in the map file (map.jhm).
  - For every existing html file that is being referenced in toc.xml, there
    must be a no-anchor mapID entry in map.jhm file.  For example, if you have
    <mapID target="..." url="sth.html#anchor"/>, you must also have <mapID
    target="..." url="sth.html"/>.
  - In toc.xml, for every <tocitem> that has a "url" pointing to a URL
    with an anchor, You'd better have a no-anchor "url" <tocitem> too.
    Otherwise the context-sensitive help may not work.  The
    context-sensitive help always jumpt to the topic with no anchor.  (I
    tried to use MastheadModel.setHelpAnchor(String) but with no luck.)

