# Android_pan_zoom_bitmap_view
<p>
Extends android.view.View to show an android.graphics.Bitmap.
</p>

<p>
It supports panning and zooming under touch and/or API control.
It checks the view dimensions in onDraw(), allowing it to automatically keep the same image center point and zoom factor while responding to changes of view height and width (such as when the android switches between portrait and landscape orientations).
</p>

<p>
This project contains code I wrote working for eCompliance Inc, and is released under the Apache license (v2) with eCompliance's permission.
</p>
