GIFMakerDemo
===

一个使用多张图片合成gif文件的demo。

使用了该库：[https://github.com/nbadal/android-gif-encoder](https://github.com/nbadal/android-gif-encoder)

GIF合成时需要注意的地方：

1. 开始生成gif的时候，是以第一张图片的尺寸生成gif图的大小，后面几张图片会基于第一张图片的尺寸进行裁切。所以要生成尺寸完全匹配的gif图的话，应先调整传入图片的尺寸，让其尺寸相同；
1. 如果传入的单张图片太大的话会造成OOM，可在不损失图片清晰度先对图片进行质量压缩。

具体内容见代码与注释。