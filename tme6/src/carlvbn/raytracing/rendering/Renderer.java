package carlvbn.raytracing.rendering;

import carlvbn.raytracing.pixeldata.Color;
import carlvbn.raytracing.pixeldata.GaussianBlur;
import carlvbn.raytracing.pixeldata.PixelBuffer;
import carlvbn.raytracing.pixeldata.PixelData;
import carlvbn.raytracing.math.*;
import carlvbn.raytracing.solids.Solid;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Renderer {
    private static final float GLOBAL_ILLUMINATION = 0.3F;
    private static final float SKY_EMISSION = 0.5F;
    private static final int MAX_REFLECTION_BOUNCES = 5;
    private static final boolean SHOW_SKYBOX = true;

    public static float bloomIntensity = 0.5F;
    public static int bloomRadius = 10;
    private static final int POOL_THREADS = Math.max(1, Runtime.getRuntime().availableProcessors());
    private static final int POOL_QUEUE_CAPACITY = 100;
    private static final ThreadPool POOL = new ThreadPool(POOL_THREADS, POOL_QUEUE_CAPACITY);
    private static final ExecutorService EXEC_SINGLE = Executors.newSingleThreadExecutor();
    private static final ExecutorService EXEC_FIXED = Executors.newFixedThreadPool(POOL_THREADS);
    private static final ExecutorService EXEC_PER_TASK = new ThreadPerTaskExecutor();

    private enum RenderMode {
        SEQUENTIAL,
        THREAD_PER_PIXEL_SYNC,
        THREAD_PER_COL_SYNC,
        POOL_COL_SYNC,
        POOL_COL_IMAGE,
        POOL_PIXEL_IMAGE,
        EXECUTOR_SINGLE_COL_IMAGE,
        EXECUTOR_FIXED_COL_IMAGE,
        EXECUTOR_PER_TASK_COL_IMAGE
    }

    // Change this value to compare implementations for TME6/TME7.
    private static final RenderMode RENDER_MODE = RenderMode.EXECUTOR_FIXED_COL_IMAGE;

    /** Renders the scene to a java.awt.Graphics object
     * @param scene The scene to Render
     * @param width The width of the desired output
     * @param height The height of the desired output
     * @param resolution (Floating point greater than 0 and lower or equal to 1) Controls the number of rays traced. (1 = Every pixel is ray-traced)
     */
    public static void renderScene(Scene scene, Graphics gfx, int width, int height, float resolution) {
        switch (RENDER_MODE) {
            case THREAD_PER_PIXEL_SYNC:
                renderSceneThreadPerPixel(scene, gfx, width, height, resolution);
                break;
            case THREAD_PER_COL_SYNC:
                renderSceneThreadPerCol(scene, gfx, width, height, resolution);
                break;
            case POOL_COL_SYNC:
                renderScenePoolCol(scene, gfx, width, height, resolution);
                break;
            case POOL_COL_IMAGE:
                renderScenePoolColV2(scene, gfx, width, height, resolution);
                break;
            case POOL_PIXEL_IMAGE:
                renderScenePoolPixelV2(scene, gfx, width, height, resolution);
                break;
            case EXECUTOR_SINGLE_COL_IMAGE:
                renderSceneExecutorCol(scene, gfx, width, height, resolution, EXEC_SINGLE, "executor-single-col");
                break;
            case EXECUTOR_FIXED_COL_IMAGE:
                renderSceneExecutorCol(scene, gfx, width, height, resolution, EXEC_FIXED, "executor-fixed-col");
                break;
            case EXECUTOR_PER_TASK_COL_IMAGE:
                renderSceneExecutorCol(scene, gfx, width, height, resolution, EXEC_PER_TASK, "executor-per-task-col");
                break;
            case SEQUENTIAL:
            default:
                renderSceneSequential(scene, gfx, width, height, resolution);
                break;
        }
    }

    public static void renderSceneSequential(Scene scene, Graphics gfx, int width, int height, float resolution) {
        int blockSize = blockSizeForResolution(resolution);
        long start = System.currentTimeMillis();
        for (int x = 0; x < width; x += blockSize) {
            for (int y = 0; y < height; y += blockSize) {
                drawPixelToGraphics(scene, gfx, width, height, x, y, blockSize);
            }
        }
        logRenderTime("sequential", start);
    }

    public static void renderSceneThreadPerPixel(Scene scene, Graphics gfx, int width, int height, float resolution) {
        int blockSize = blockSizeForResolution(resolution);
        long start = System.currentTimeMillis();
        List<Thread> threads = new ArrayList<>();
        for (int x = 0; x < width; x += blockSize) {
            for (int y = 0; y < height; y += blockSize) {
                final int fx = x;
                final int fy = y;
                Thread thread = new Thread(() -> drawPixelToGraphicsSync(scene, gfx, width, height, fx, fy, blockSize));
                threads.add(thread);
                thread.start();
            }
        }
        joinAll(threads);
        logRenderTime("thread-per-pixel-sync", start);
    }

    public static void renderSceneThreadPerCol(Scene scene, Graphics gfx, int width, int height, float resolution) {
        int blockSize = blockSizeForResolution(resolution);
        long start = System.currentTimeMillis();
        List<Thread> threads = new ArrayList<>();
        for (int x = 0; x < width; x += blockSize) {
            final int fx = x;
            Thread thread = new Thread(() -> {
                for (int y = 0; y < height; y += blockSize) {
                    drawPixelToGraphicsSync(scene, gfx, width, height, fx, y, blockSize);
                }
            });
            threads.add(thread);
            thread.start();
        }
        joinAll(threads);
        logRenderTime("thread-per-col-sync", start);
    }

    public static void renderScenePoolCol(Scene scene, Graphics gfx, int width, int height, float resolution) {
        int blockSize = blockSizeForResolution(resolution);
        long start = System.currentTimeMillis();
        int tasks = columnTaskCount(width, blockSize);
        CountDownLatch latch = new CountDownLatch(tasks);
        for (int x = 0; x < width; x += blockSize) {
            final int fx = x;
            POOL.execute(() -> {
                try {
                    for (int y = 0; y < height; y += blockSize) {
                        drawPixelToGraphicsSync(scene, gfx, width, height, fx, y, blockSize);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        awaitLatch(latch);
        logRenderTime("pool-col-sync", start);
    }

    public static void renderScenePoolColV2(Scene scene, Graphics gfx, int width, int height, float resolution) {
        int blockSize = blockSizeForResolution(resolution);
        long start = System.currentTimeMillis();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int tasks = columnTaskCount(width, blockSize);
        CountDownLatch latch = new CountDownLatch(tasks);
        for (int x = 0; x < width; x += blockSize) {
            final int fx = x;
            POOL.execute(() -> {
                try {
                    for (int y = 0; y < height; y += blockSize) {
                        drawPixelToImage(scene, image, width, height, fx, y, blockSize);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        awaitLatch(latch);
        gfx.drawImage(image, 0, 0, null);
        logRenderTime("pool-col-image", start);
    }

    public static void renderScenePoolPixelV2(Scene scene, Graphics gfx, int width, int height, float resolution) {
        int blockSize = blockSizeForResolution(resolution);
        long start = System.currentTimeMillis();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int tasks = pixelTaskCount(width, height, blockSize);
        CountDownLatch latch = new CountDownLatch(tasks);
        for (int x = 0; x < width; x += blockSize) {
            for (int y = 0; y < height; y += blockSize) {
                final int fx = x;
                final int fy = y;
                POOL.execute(() -> {
                    try {
                        drawPixelToImage(scene, image, width, height, fx, fy, blockSize);
                    } finally {
                        latch.countDown();
                    }
                });
            }
        }
        awaitLatch(latch);
        gfx.drawImage(image, 0, 0, null);
        logRenderTime("pool-pixel-image", start);
    }

    public static void renderSceneExecutorCol(
            Scene scene,
            Graphics gfx,
            int width,
            int height,
            float resolution,
            ExecutorService exec,
            String modeName
    ) {
        int blockSize = blockSizeForResolution(resolution);
        long start = System.currentTimeMillis();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int tasks = columnTaskCount(width, blockSize);
        CountDownLatch latch = new CountDownLatch(tasks);
        for (int x = 0; x < width; x += blockSize) {
            final int fx = x;
            exec.execute(() -> {
                try {
                    for (int y = 0; y < height; y += blockSize) {
                        drawPixelToImage(scene, image, width, height, fx, y, blockSize);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        awaitLatch(latch);
        gfx.drawImage(image, 0, 0, null);
        logRenderTime(modeName, start);
    }

    /** Same as the above but applies Post-Processing effects before drawing. */
    public static void renderScenePostProcessed(Scene scene, Graphics gfx, int width, int height, float resolution) {
        int bufferWidth = Math.round(width*resolution+0.49F);
        int bufferHeight = Math.round(height*resolution+0.49F);
        PixelBuffer pixelBuffer  = renderScene(scene, bufferWidth, bufferHeight);

        PixelBuffer emissivePixels = pixelBuffer.clone(); // The width of this buffer has to remain constant to keep the blur factor the same for all sizes
        emissivePixels.filterByEmission(0.1F);
        GaussianBlur blur = new GaussianBlur(emissivePixels);
        blur.blur(bloomRadius, 1);
        pixelBuffer.add(blur.getPixelBuffer().multiply(bloomIntensity).resize(bufferWidth, bufferHeight, true));

        float blockSize = 1/resolution;
        for (int x = 0; x<bufferWidth; x++) {
            for (int y = 0; y < bufferHeight; y++) {
                PixelData pixel = pixelBuffer.getPixel(x, y);
                gfx.setColor(pixel.getColor().toAWTColor());
                gfx.fillRect((int)(x*blockSize), (int)(y*blockSize), (int)blockSize+1, (int)blockSize+1);
            }
        }

    }

    /** Get OpenGL style screen coordinates where (0,0) is the center of the screen from x, y coordinates that start from the top-left corner of the screen */
    public static float[] getNormalizedScreenCoordinates(int x, int y, int width, int height) {
        float u = 0, v = 0;
        if (width > height) {
            u = (float)(x - width/2+height/2) / height * 2 - 1;
            v =  -((float) y / height * 2 - 1);
        } else {
            u = (float)x / width * 2 - 1;
            v =  -((float) (y - height/2+width/2) / width * 2 - 1);
        }

        return new float[]{u, v};
    }

    public static PixelData computePixelInfo(Scene scene, float u, float v) {
        Vector3 eyePos = new Vector3(0,0, (float)(-1/Math.tan(Math.toRadians(scene.getCamera().getFOV()/2))));
        Camera cam = scene.getCamera();

        Vector3 rayDir = new Vector3(u, v, 0).subtract(eyePos).normalize().rotateYP(cam.getYaw(), cam.getPitch());
        RayHit hit = scene.raycast(new Ray(eyePos.add(cam.getPosition()), rayDir));
        if (hit != null) {
            return computePixelInfoAtHit(scene, hit, MAX_REFLECTION_BOUNCES);
        } else if (SHOW_SKYBOX) {
            Color sbColor = scene.getSkybox().getColor(rayDir);
            return new PixelData(sbColor, Float.POSITIVE_INFINITY, sbColor.getLuminance()*SKY_EMISSION);
        } else {
            return new PixelData(Color.BLACK, Float.POSITIVE_INFINITY, 0);
        }
    }

    private static PixelData computePixelInfoAtHit(Scene scene, RayHit hit, int recursionLimit) {
        Vector3 hitPos = hit.getPosition();
        Vector3 rayDir = hit.getRay().getDirection();
        Solid hitSolid = hit.getSolid();
        Color hitColor = hitSolid.getTextureColor(hitPos.subtract(hitSolid.getPosition()));
        float brightness = getDiffuseBrightness(scene, hit);
        float specularBrightness = getSpecularBrightness(scene, hit);
        float reflectivity = hitSolid.getReflectivity();
        float emission = hitSolid.getEmission();

        PixelData reflection;
        Vector3 reflectionVector = rayDir.subtract(hit.getNormal().multiply(2*Vector3.dot(rayDir, hit.getNormal())));
        Vector3 reflectionRayOrigin = hitPos.add(reflectionVector.multiply(0.001F)); // Add a little to avoid hitting the same solid again
        RayHit reflectionHit = recursionLimit > 0 ? scene.raycast(new Ray(reflectionRayOrigin, reflectionVector)) : null;
        if (reflectionHit != null) {
            reflection = computePixelInfoAtHit(scene, reflectionHit, recursionLimit-1);
        } else {
            Color sbColor = scene.getSkybox().getColor(reflectionVector);
            reflection = new PixelData(sbColor, Float.POSITIVE_INFINITY, sbColor.getLuminance()*SKY_EMISSION);
        }

        Color pixelColor = Color.lerp(hitColor, reflection.getColor(), reflectivity) // Reflected color
                .multiply(brightness) // Diffuse lighting
                .add(specularBrightness) // Specular lighting
                .add(hitColor.multiply(emission)) // Object emission
                .add(reflection.getColor().multiply(reflection.getEmission()*reflectivity)); // Indirect illumination

        return new PixelData(pixelColor, Vector3.distance(scene.getCamera().getPosition(), hitPos), Math.min(1, emission+reflection.getEmission()*reflectivity+specularBrightness));
    }

    private static float getDiffuseBrightness(Scene scene, RayHit hit) {
        Light sceneLight = scene.getLight();

        // Raytrace to light to check if something blocks the light
        RayHit lightBlocker = scene.raycast(new Ray(sceneLight.getPosition(), hit.getPosition().subtract(sceneLight.getPosition()).normalize()));
        if (lightBlocker != null && lightBlocker.getSolid() != hit.getSolid()) {
            return GLOBAL_ILLUMINATION; // GOBAL_ILLUMINATION = Minimum brightness
        } else {
            return Math.max(GLOBAL_ILLUMINATION, Math.min(1, Vector3.dot(hit.getNormal(), sceneLight.getPosition().subtract(hit.getPosition()))));
        }
    }

    private static float getSpecularBrightness(Scene scene, RayHit hit) {
        Vector3 hitPos = hit.getPosition();
        Vector3 cameraDirection = scene.getCamera().getPosition().subtract(hitPos).normalize();
        Vector3 lightDirection = hitPos.subtract(scene.getLight().getPosition()).normalize();
        Vector3 lightReflectionVector = lightDirection.subtract(hit.getNormal().multiply(2*Vector3.dot(lightDirection, hit.getNormal())));

        float specularFactor = Math.max(0, Math.min(1, Vector3.dot(lightReflectionVector, cameraDirection)));
        return (float) Math.pow(specularFactor, 2)*hit.getSolid().getReflectivity();
    }
    
    /** ADDITIONS FOR THE COURSE */
    /** Unsynchronized version of fillRect with a Color and example usage. */

    /** Renders the scene to a Pixel buffer (example usage)
     * @param scene The scene to Render
     * @param width The width of the desired output
     * @param height The height of the desired output
     * @return The rendered PixelBuffer
     */
    public static PixelBuffer renderScene(Scene scene, int width, int height) {
        PixelBuffer pixelBuffer = new PixelBuffer(width, height);

        for (int x = 0; x<width; x++) {
            for (int y = 0; y<height; y++) {
                float[] screenUV = getNormalizedScreenCoordinates(x, y, width, height);

                pixelBuffer.setPixel(x, y, computePixelInfo(scene, screenUV[0], screenUV[1]));
            }
        }

        return pixelBuffer;
    }

    /**
     * Fills a rectangle area of a BufferedImage with a specified color.
     * This function is thread-safe as long as no two threads write to the same pixels.
     *
     * @param image The BufferedImage to modify
     * @param x The x-coordinate of the rectangle's top-left corner
     * @param y The y-coordinate of the rectangle's top-left corner
     * @param width The width of the rectangle
     * @param height The height of the rectangle
     * @param color The color to fill with
     */
    public static void fillColorRect(BufferedImage image, int x, int y, int width, int height, Color color) {
        int rgb = color.getRGB();
        int imgWidth = image.getWidth();
        int imgHeight = image.getHeight();

        // Ensure the rectangle is within the image bounds
        int startX = Math.max(0, x);
        int startY = Math.max(0, y);
        int endX = Math.min(x + width, imgWidth);
        int endY = Math.min(y + height, imgHeight);

        if (startX >= endX || startY >= endY) {
            // Rectangle is outside the image bounds; nothing to fill
            return;
        }

        // Get the pixel data array
        int[] pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();

        // Fill the rectangle area
        for (int row = startY; row < endY; row++) {
            int offset = row * imgWidth + startX;
            int length = endX - startX;
            Arrays.fill(pixels, offset, offset + length, rgb);
        }
    }

    private static int blockSizeForResolution(float resolution) {
        int blockSize = (int) (1 / resolution);
        return Math.max(1, blockSize);
    }

    private static void drawPixelToGraphics(Scene scene, Graphics gfx, int width, int height, int x, int y, int blockSize) {
        float[] uv = getNormalizedScreenCoordinates(x, y, width, height);
        PixelData pixelData = computePixelInfo(scene, uv[0], uv[1]);
        gfx.setColor(pixelData.getColor().toAWTColor());
        gfx.fillRect(x, y, blockSize, blockSize);
    }

    private static void drawPixelToGraphicsSync(Scene scene, Graphics gfx, int width, int height, int x, int y, int blockSize) {
        float[] uv = getNormalizedScreenCoordinates(x, y, width, height);
        PixelData pixelData = computePixelInfo(scene, uv[0], uv[1]);
        synchronized (gfx) {
            gfx.setColor(pixelData.getColor().toAWTColor());
            gfx.fillRect(x, y, blockSize, blockSize);
        }
    }

    private static void drawPixelToImage(Scene scene, BufferedImage image, int width, int height, int x, int y, int blockSize) {
        float[] uv = getNormalizedScreenCoordinates(x, y, width, height);
        PixelData pixelData = computePixelInfo(scene, uv[0], uv[1]);
        fillColorRect(image, x, y, blockSize, blockSize, pixelData.getColor());
    }

    private static int columnTaskCount(int width, int blockSize) {
        return (width + blockSize - 1) / blockSize;
    }

    private static int pixelTaskCount(int width, int height, int blockSize) {
        return columnTaskCount(width, blockSize) * columnTaskCount(height, blockSize);
    }

    private static void joinAll(List<Thread> threads) {
        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Rendering interrupted while joining threads", e);
            }
        }
    }

    private static void awaitLatch(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Rendering interrupted while waiting for tasks", e);
        }
    }

    private static void logRenderTime(String mode, long start) {
        long duration = System.currentTimeMillis() - start;
        System.out.println("Rendered [" + mode + "] in " + duration + "ms");
    }

    private static class ThreadPerTaskExecutor implements ExecutorService {
        @Override
        public void execute(Runnable command) {
            Thread t = new Thread(command, "renderer-exec-per-task");
            t.setDaemon(true);
            t.start();
        }

        @Override public void shutdown() {}
        @Override public List<Runnable> shutdownNow() { return new ArrayList<>(); }
        @Override public boolean isShutdown() { return false; }
        @Override public boolean isTerminated() { return false; }
        @Override public boolean awaitTermination(long timeout, TimeUnit unit) { return true; }
        @Override public <T> java.util.concurrent.Future<T> submit(java.util.concurrent.Callable<T> task) { throw new UnsupportedOperationException(); }
        @Override public <T> java.util.concurrent.Future<T> submit(Runnable task, T result) { throw new UnsupportedOperationException(); }
        @Override public java.util.concurrent.Future<?> submit(Runnable task) { throw new UnsupportedOperationException(); }
        @Override public <T> List<java.util.concurrent.Future<T>> invokeAll(java.util.Collection<? extends java.util.concurrent.Callable<T>> tasks) { throw new UnsupportedOperationException(); }
        @Override public <T> List<java.util.concurrent.Future<T>> invokeAll(java.util.Collection<? extends java.util.concurrent.Callable<T>> tasks, long timeout, TimeUnit unit) { throw new UnsupportedOperationException(); }
        @Override public <T> T invokeAny(java.util.Collection<? extends java.util.concurrent.Callable<T>> tasks) { throw new UnsupportedOperationException(); }
        @Override public <T> T invokeAny(java.util.Collection<? extends java.util.concurrent.Callable<T>> tasks, long timeout, TimeUnit unit) { throw new UnsupportedOperationException(); }
    }
}
