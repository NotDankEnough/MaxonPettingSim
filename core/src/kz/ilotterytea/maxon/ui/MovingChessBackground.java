package kz.ilotterytea.maxon.ui;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class MovingChessBackground {
    private final Logger log = LoggerFactory.getLogger(MovingChessBackground.class.getSimpleName());

    private final float velocityX;
    private final float velocityY;
    private float screenWidth, screenHeight;
    private final List<Drawable> drawables;

    private final ArrayList<ArrayList<Image>> tiles;

    /**
     * Background that looking like chess and moves.
     * @param velocityX X Velocity
     * @param velocityY Y Velocity
     * @param screenWidth Width of the screen
     * @param screenHeight Height of the screen
     * @param drawables Drawables to draw
     */
    public MovingChessBackground(
            float velocityX,
            float velocityY,
            float screenWidth,
            float screenHeight,
            ArrayList<Drawable> drawables
    ) {
        this.tiles = new ArrayList<>();
        this.drawables = drawables;

        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;

        update(screenWidth, screenHeight);
    }

    /**
     * Clean up the background tiles and create new ones.
     * @param width Width of the screen.
     * @param height Height of the screen.
     */
    public void update(float width, float height) {
        screenWidth = width;
        screenHeight = height;

        log.info("Starting to update the \"Moving chess\" background...");
        tiles.clear();
        log.info("List of tiles is cleared!");

        float totalDWidth = 0, totalDHeight = 0;

        for (Drawable drawable : drawables) {
            totalDWidth += 64;
            totalDHeight += 64;
        }

        totalDWidth = totalDWidth / drawables.size();
        totalDHeight = totalDHeight / drawables.size();

        log.info("Total size of {} drawables: {}x{}", drawables.size(), totalDWidth, totalDHeight);

        int DIndex = 0;

        log.info("Starting to generating tiles...");

        for (int h = 0; h < height / totalDHeight + 3; h++) {
            tiles.add(h, new ArrayList<>());

            for (int w = -1; w < width / totalDWidth; w++) {
                if (DIndex + 1 > drawables.size()) DIndex = 0;
                Image tile = new Image(drawables.get(DIndex++));
                tile.setSize(64f, 64f);

                tile.setPosition(tile.getWidth() * w, tile.getHeight() * h);

                tiles.get(h).add(tile);
            }
        }

        log.info("\"Moving chess\" background is successfully updated!");
    }

    /**
     * Draw the background tiles.
     * @param batch Sprite batch.
     */
    public void draw(
            SpriteBatch batch
    ) {
        ArrayList<ArrayList<Image>> outYSprites = new ArrayList<>();

        // For horizontal:
        for (ArrayList<Image> array : tiles) {
            for (Image tile : array) {
                tile.setPosition(tile.getX() + velocityX, tile.getY() + velocityY);
                tile.draw(batch, 1f);

                if (tile.getX() > screenWidth) {
                    Image fTile = array.get(0);

                    tile.setPosition(fTile.getX() - tile.getWidth(), fTile.getY());

                    if (array.size() > 1 && tile.getDrawable() == fTile.getDrawable()) {
                        tile.setDrawable(array.get(1).getDrawable());
                    }

                    array.remove(tile);
                    array.add(0, tile);
                }

                if (!outYSprites.contains(array) && tile.getY() > screenHeight) {
                    outYSprites.add(array);
                }
            }
        }

        // For vertical:
        for (ArrayList<Image> array : outYSprites) {
            int index = 0;

            for (Image tile : array) {
                if (index + 1 > tiles.get(0).size()) index = 0;
                Image fTile = tiles.get(0).get(index++);
                tile.setPosition(tile.getX(), fTile.getY() - tile.getHeight());

                if (fTile.getDrawable() == tile.getDrawable()) {
                    if (index + 1 > tiles.get(0).size()) index = 0;
                    tile.setDrawable(tiles.get(0).get(index).getDrawable());
                }
            }

            tiles.remove(array);
            tiles.add(0, array);
        }

        outYSprites.clear();
    }
}