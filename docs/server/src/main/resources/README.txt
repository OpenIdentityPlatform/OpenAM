The PlantUML sequence diagrams, oauth*.txt, use double-arrowed messages
to represent multi-message interchanges.

For example:

Owner<->AuthzServer: Authenticate resource owner and\nconfirm resource access

Because the version of the PlantUML Maven artifact that we use, 7940,
does not yet support <->, the .png files must be built separately.

Once we can move to a more recent PlantUML artifact,
we should move these files to src/main/docbkx/admin-guide/images,
replacing the corresponding .png files,
so that the sequence diagrams are generated from source.
