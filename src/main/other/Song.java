package main.other;

import main.handlers.OggClip;
import org.ini4j.Ini;
import org.ini4j.Profile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by osum4est on 10/5/16.
 */
public class Song {
    private String title;
    public String getTitle() { return title; }

    private String artist;
    public String getArtist() { return artist; }

    private String album;
    public String getAlbum() { return album; }

    private String enclosingFolder;
    public String getEnclosingFolder() { return enclosingFolder; }

    private Path dir;
    public Path getDir() { return dir; }

    private boolean inLibrary;
    public boolean getInLibrary() { return inLibrary; }

    private String genre;
    public String getGenre() { return genre; }
    private String year;
    public String getYear() { return year; }
    private String band;
    public String getBand() { return band; }
    private String guitar;
    public String getGuitar() { return guitar; }
    private String vocals;
    public String getVocals() { return vocals; }
    private String drums;
    public String getDrums() { return drums; }
    private String bass;
    public String getBass() { return bass; }
    private String keys;
    public String getKeys() { return keys; }

    private ArrayList<OggClip> clips;

    public Song(Ini ini, Path dir, boolean inLibrary)
    {
        clips = new ArrayList<OggClip>();

        Profile.Section section = ini.get("song");
        title = section.get("name");
        artist = section.get("artist");
        album = section.get("album");
        this.enclosingFolder = dir.getParent().getFileName().toString();
        this.dir = dir;
        this.inLibrary = inLibrary;
        genre = section.get("genre");
        year = section.get("year");
        band = section.get("diff_band");
        if (band == null || band.equals("-1"))
            band = "N/A";
        guitar = section.get("diff_guitar");
        if (guitar == null || guitar.equals("-1"))
            guitar = "N/A";
        vocals = section.get("diff_vocals");
        if (vocals == null || vocals.equals("-1"))
            vocals = "N/A";
        drums = section.get("diff_drums");
        if (drums == null || drums.equals("-1"))
            drums = "N/A";
        bass = section.get("diff_bass");
        if (bass == null || bass.equals("-1"))
            bass = "N/A";
        keys = section.get("diff_keys");
        if (keys == null || keys.equals("-1"))
            keys = "N/A";
    }

    public void play() {
        for (File file : dir.toFile().listFiles()) {
            try {
                if (file.getName().endsWith("preview.ogg")) {
                    clips.clear();
                    clips.add(new OggClip(file.getPath()));
                    break;
                }
                if (file.getPath().endsWith(".ogg"))
                    clips.add(new OggClip(file.getPath()));
            } catch (Exception e) {
                System.out.println("Could not play " + file.getName());
            }
        }
        for (OggClip clip : clips)
            clip.play();
    }

    public void stop() {
        for (OggClip clip : clips)
            clip.pause();
        for (OggClip clip : clips)
            clip.stop();
    }

    public void setVolume(float vol) {
        for (OggClip clip : clips)
            clip.setGain((float)Math.pow(vol / 100, .5f));
    }

    public void addToLibrary(String dest) {
        if (!inLibrary) {
            try {
                Files.createSymbolicLink(new File(dest + "/" + getDir().getFileName()).toPath(), getDir());
                inLibrary = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void removeFromLibrary(String dest) {
        try {
            Files.deleteIfExists(new File(dest + "/" + getDir().getFileName()).toPath());
            inLibrary = false;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
