// FIXME: license file if you have one

package android.hardware.oem.customizescanservice;

@VintfStability
interface ICustomizeScanServiceCallback {
	/**
	 *Description: the callback of scm1 data.
	 *@width: int
	 *@height: int
	 *@stride: int
	 *@size: long
	 *@shared_fd: ParcelFileDescriptor  it save the fd of mmap
	 *@idx: int  user need release buffer depend on idx after getting the data of camera. you can execute the interface customize_cam_scm1_return_buffer
	 **/
    void OnDataCallBackFdScm1(int width,int height,int stride,long size,inout ParcelFileDescriptor shared_fd,int idx);

	/**
	 *Description: the callback of scm2 data.
	 *@width: int
	 *@height: int
	 *@stride: int
	 *@size: long
	 *@shared_fd: ParcelFileDescriptor  it save the fd of mmap
	 *@idx: int  user need release buffer depend on idx after getting the data of camera. you can execute the interface customize_cam_scm2_return_buffer
	 **/
    void OnDataCallBackFdScm2(int width,int height,int stride,long size,inout ParcelFileDescriptor shared_fd,int idx);
	
	/**
	 *Description: the callback of ccm data.
	 *@width: int
	 *@height: int
	 *@stride: int
	 *@size: long
	 *@shared_fd: ParcelFileDescriptor  it save the fd of mmap
	 *@idx: int  user need release buffer depend on idx after getting the data of camera. you can execute the interface customize_cam_ccm_return_buffer
	 **/
    void OnDataCallBackFdCcm(int width,int height,int stride,long size,inout ParcelFileDescriptor shared_fd,int idx);
	
	/**
	 *Description: the callback of quad scm data.
	 *@width: int
	 *@height: int
	 *@stride: int
	 *@size: long
	 *@shared_fd: ParcelFileDescriptor  it save the fd of mmap
	 *@idx: int  user need release buffer depend on idx after getting the data of camera. you can execute the interface customize_quad_cam_scm_return_buffer
	 **/
	void OnDataCallBackFdQuadScm(int width,int height,int stride,long size,inout ParcelFileDescriptor shared_fd,int idx);
}