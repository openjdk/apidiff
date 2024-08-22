/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package apitest;

/**
 * A class to provide a chunk of "lorem ipsum" text,
 * and to encapsulate the spelling errors inherent therein!
 */
public class LoremIpsum {
    static final String text;

    static {
        text = """
            Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et \
            dolore magna aliqua. Dolor sed viverra ipsum nunc aliquet bibendum enim. In massa tempor nec feugiat. \
            Nunc aliquet bibendum enim facilisis gravida. Nisl nunc mi ipsum faucibus vitae aliquet nec ullamcorper. \
            Amet luctus venenatis lectus magna fringilla. Volutpat maecenas volutpat blandit aliquam etiam erat \
            velit scelerisque in. Egestas egestas fringilla phasellus faucibus scelerisque eleifend. Sagittis orci \
            a scelerisque purus semper eget duis. Nulla pharetra diam sit amet nisl suscipit. Sed adipiscing diam \
            donec adipiscing tristique risus nec feugiat in. Fusce ut placerat orci nulla. Pharetra vel turpis nunc \
            eget lorem dolor. Tristique senectus et netus et malesuada.\
            \
            Etiam tempor orci eu lobortis elementum nibh tellus molestie. Neque egestas congue quisque egestas. \
            Egestas integer eget aliquet nibh praesent tristique. Vulputate mi sit amet mauris. Sodales neque \
            sodales ut etiam sit. Dignissim suspendisse in est ante in. Volutpat commodo sed egestas egestas. \
            Felis donec et odio pellentesque diam. Pharetra vel turpis nunc eget lorem dolor sed viverra. \
            Porta nibh venenatis cras sed felis eget. Aliquam ultrices sagittis orci a. Dignissim diam quis enim \
            lobortis. Aliquet porttitor lacus luctus accumsan. Dignissim convallis aenean et tortor at risus \
            viverra adipiscing at.""";
    }

    private LoremIpsum() { }
}
