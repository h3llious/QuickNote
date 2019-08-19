# QuickNote
A Note Taker app

This project is intended to develop a functional note taking app.

## Implemented features
* Creating new notes, viewing, updating and deleting exist notes.
    * Swipe to delete and undo in main activity.
    * Sort notes by time modified or title, ascending or descending.
    * Search notes by title, content or date modified.
* Adding text, pictures, files into notes.
    * Add images from device and camera, add files from file chooser.
    * Delete or save attachments.
    * Insert images into EditText using Spannable.
* Synchronizing data using Drive API v3:
    * Deleting notes on Drive if synced local notes have been deleted and vice versa
    * Uploading new local notes into Drive
    * Downloading notes from Drive into local database
    * Updating notes if there have been changes
* Simple, user-friendly layout.
    * Splash Screen
    * Coordinator Layout for 'Editing notes' activity.

## Planned features
* Simple, easy to use layout yet having necessary functions.
* Improved performance.

## Screenshots

| | | |
|:-------------------------:|:-------------------------:|:-------------------------:|
|<img src="./screenshots/splash.jpg" width="360"  /> Splash Screen|<img src="./screenshots/empty.jpg" width="360" /> Empty list|<img src="./screenshots/noteslist.jpg" width="360"  /> Note list|
|<img src="./screenshots/search.jpg" width="360"  /> Search for notes|<img src="./screenshots/swipe.jpg" width="360"  /> Swipe to delete|<img src="./screenshots/undo.jpg" width="360"  /> Undo|
|<img src="./screenshots/sort.jpg" width="360"  /> Sort|<img src="./screenshots/navdrawer.jpg" width="360" /> Navigation Drawer|<img src="./screenshots/syncing.jpg" width="360"   /> Synchronizting|
|<img src="./screenshots/finishsyncing.jpg" width="360" /> Finish syncing|<img src="./screenshots/newnote.jpg" width="360"  /> New note|<img src="./screenshots/existednote.jpg" width="360"  /> View existed note|
|<img src="./screenshots/imagespan.jpg" width="360"  /> Add images to content|<img src="./screenshots/popupmenu.jpg" width="360"   /> Options menu for attachments||
