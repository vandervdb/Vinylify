# Les AAR Spotify embarquent leurs propres règles consommateur.
-keep class com.spotify.** { *; }
-dontwarn com.spotify.**
