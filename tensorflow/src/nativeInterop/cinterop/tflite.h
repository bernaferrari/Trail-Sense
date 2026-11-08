#include <stddef.h>
#include <stdint.h>
typedef struct TfLiteModel TfLiteModel;
typedef struct TfLiteInterpreter TfLiteInterpreter;
typedef struct TfLiteInterpreterOptions TfLiteInterpreterOptions;
typedef struct TfLiteTensor TfLiteTensor;
TfLiteModel* TfLiteModelCreateFromFile(const char* model_path);
void TfLiteModelDelete(TfLiteModel* model);
TfLiteInterpreter* TfLiteInterpreterCreate(const TfLiteModel* model, const TfLiteInterpreterOptions* options);
void TfLiteInterpreterDelete(TfLiteInterpreter* interpreter);
int TfLiteInterpreterAllocateTensors(TfLiteInterpreter* interpreter);
int32_t TfLiteInterpreterGetInputTensorCount(const TfLiteInterpreter* interpreter);
int32_t TfLiteInterpreterGetOutputTensorCount(const TfLiteInterpreter* interpreter);
TfLiteTensor* TfLiteInterpreterGetInputTensor(TfLiteInterpreter* interpreter, int32_t input_index);
const TfLiteTensor* TfLiteInterpreterGetOutputTensor(const TfLiteInterpreter* interpreter, int32_t output_index);
size_t TfLiteTensorByteSize(const TfLiteTensor* tensor);
int TfLiteTensorCopyFromBuffer(TfLiteTensor* tensor, const void* input_data, size_t input_data_size);
int TfLiteTensorCopyToBuffer(const TfLiteTensor* tensor, void* output_data, size_t output_data_size);
int TfLiteInterpreterInvoke(TfLiteInterpreter* interpreter);
