/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.presto.orc;

import io.airlift.compress.Compressor;
import io.airlift.slice.Slice;

import java.nio.ByteBuffer;

import static java.util.Objects.requireNonNull;

public class EncryptingCompressor
        implements Compressor
{
    private DwrfEncryptor dwrfEncryptor;
    private Compressor compressor;

    public EncryptingCompressor(DwrfEncryptor dwrfEncryptor, Compressor compressor)
    {
        this.dwrfEncryptor = requireNonNull(dwrfEncryptor, "dwrfEncrpytor is null");
        this.compressor = requireNonNull(compressor, "compressor is null");
    }

    @Override
    public int maxCompressedLength(int uncompressedSize)
    {
        return dwrfEncryptor.maxEncryptedLength(compressor.maxCompressedLength(uncompressedSize));
    }

    @Override
    public int compress(byte[] input, int inputOffset, int inputLength, byte[] output, int outputOffset, int maxOutputLength)
    {
        byte[] compressorOutput = new byte[maxOutputLength];
        int compressedSize = compressor.compress(input, inputOffset, inputLength, compressorOutput, 0, maxOutputLength);
        Slice encryptedSlice = dwrfEncryptor.encrypt(compressorOutput, 0, compressedSize);
        ByteBuffer outputBuffer = ByteBuffer.wrap(output, outputOffset, maxOutputLength);
        outputBuffer.put(encryptedSlice.byteArray(), encryptedSlice.byteArrayOffset(), encryptedSlice.length());
        return outputBuffer.position() - outputOffset;
    }

    @Override
    public void compress(ByteBuffer input, ByteBuffer output)
    {
        throw new UnsupportedOperationException("not supported for encrypting compressor");
    }
}