/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package com.sun.javafx.image;

import java.nio.IntBuffer;

public interface IntToIntPixelConverter
    extends PixelConverter<IntBuffer, IntBuffer>
{
    /**
     * Copies a rectangular region of data from the source array to the
     * destination array using the following relationship:
     * <pre>
     * for each xy : 0 <= x,y < w,h {
     *     int srcpos = y * srcscanints + x * srcintsperpixel + srcoff;
     *     int dstpos = y * dstscanints + x * dstintsperpixel + dstoff;
     *     for each j : 0 <= j < srcintsperpixel {
     *         load data from srcarr[srcpos + j];
     *     }
     *     convert data to destination pixel format
     *     for each k : 0 <= k < dstintsperpixel {
     *         store data into dstarr[dstpos + k] = pixel data;
     *     }
     * }
     * </pre>
     * 
     * @param srcbuf the int array containing the source data
     * @param srcoff the index in the array of the first source pixel data
     * @param srcscanints number of array indices between rows of data in the source
     * @param dstbuf the int array containing the destination data
     * @param dstoff the index in the array of the first destination pixel data
     * @param dstscanints number of array indices between rows of data in the destination
     * @param w the number of pixels to process across before moving to the next row
     * @param h the number of rows of pixels to process
     */
    public void convert(int srcarr[], int srcoff, int srcscanints,
                        int dstarr[], int dstoff, int dstscanints,
                        int w, int h);

    /**
     * Copies a rectangular region of data from the source buffer to the
     * destination array using the following relationship:
     * <pre>
     * for each xy : 0 <= x,y < w,h {
     *     int srcpos = y * srcscanints + x * srcintsperpixel + srcoff;
     *     int dstpos = y * dstscanints + x * dstintsperpixel + dstoff;
     *     for each j : 0 <= j < srcintsperpixel {
     *         load data from srcbuf.get(srcpos + j);
     *     }
     *     convert data to destination pixel format
     *     for each k : 0 <= k < dstintsperpixel {
     *         store data into dstarr[dstpos + k] = pixel data;
     *     }
     * }
     * </pre>
     * 
     * @param srcbuf the nio buffer containing the source data
     * @param srcoff the absolute location in the buffer of the first source pixel data
     * @param srcscanints number of buffer elements between rows of data in the source
     * @param dstbuf the int array containing the destination data
     * @param dstoff the index in the array of the first destination pixel data
     * @param dstscanints number of array indices between rows of data in the destination
     * @param w the number of pixels to process across before moving to the next row
     * @param h the number of rows of pixels to process
     */
    public void convert(IntBuffer srcbuf,   int srcoff, int srcscanints,
                        int       dstarr[], int dstoff, int dstscanints,
                        int w, int h);

    /**
     * Copies a rectangular region of data from the source array to the
     * destination buffer using the following relationship:
     * <pre>
     * for each xy : 0 <= x,y < w,h {
     *     int srcpos = y * srcscanints + x * srcintsperpixel + srcoff;
     *     int dstpos = y * dstscanints + x * dstintsperpixel + dstoff;
     *     for each j : 0 <= j < srcintsperpixel {
     *         load data from srcarr[srcpos + j];
     *     }
     *     convert data to destination pixel format
     *     for each k : 0 <= k < dstintsperpixel {
     *         store data into dstbuf.put(dstpos + k, pixel data);
     *     }
     * }
     * </pre>
     * 
     * @param srcbuf the int array containing the source data
     * @param srcoff the index in the array of the first source pixel data
     * @param srcscanints number of array indices between rows of data in the source
     * @param dstbuf the nio buffer containing the destination data
     * @param dstoff the absolute location in the buffer of the first destination pixel data
     * @param dstscanints number of buffer elements between rows of data in the destination
     * @param w the number of pixels to process across before moving to the next row
     * @param h the number of rows of pixels to process
     */
    public void convert(int       srcarr[], int srcoff, int srcscanints,
                        IntBuffer dstbuf,   int dstoff, int dstscanints,
                        int w, int h);
}
