package com.lzx.library.selector;


import com.lzx.library.bean.LocalMedia;

public interface IPreviewListData extends android.os.IInterface {

    public static class Default implements IPreviewListData {
        @Override
        public java.util.List<LocalMedia> getPreviewList() throws android.os.RemoteException {
            return null;
        }

        @Override
        public int getPosition() throws android.os.RemoteException {
            return 0;
        }

        @Override
        public android.os.IBinder asBinder() {
            return null;
        }
    }

    public static abstract class Stub extends android.os.Binder implements IPreviewListData {

        public Stub() {
            this.attachInterface(this, DESCRIPTOR);
        }

        public static IPreviewListData asInterface(android.os.IBinder obj) {
            if ((obj == null)) {
                return null;
            }
            android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (((iin != null) && (iin instanceof IPreviewListData))) {
                return ((IPreviewListData) iin);
            }
            return new Proxy(obj);
        }

        @Override
        public android.os.IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags)
                throws android.os.RemoteException {
            String descriptor = DESCRIPTOR;
            switch (code) {
                case INTERFACE_TRANSACTION: {
                    reply.writeString(descriptor);
                    return true;
                }
            }
            switch (code) {
                case TRANSACTION_getPreviewList: {
                    data.enforceInterface(descriptor);
                    java.util.List<LocalMedia> _result = this.getPreviewList();
                    reply.writeNoException();
                    reply.writeTypedList(_result);
                    return true;
                }
                case TRANSACTION_getPosition: {
                    data.enforceInterface(descriptor);
                    int _result = this.getPosition();
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                }
                default: {
                    return super.onTransact(code, data, reply, flags);
                }
            }
        }

        private static class Proxy implements IPreviewListData {
            private android.os.IBinder mRemote;

            Proxy(android.os.IBinder remote) {
                mRemote = remote;
            }

            @Override
            public android.os.IBinder asBinder() {
                return mRemote;
            }

            public String getInterfaceDescriptor() {
                return DESCRIPTOR;
            }

            @Override
            public java.util.List<LocalMedia> getPreviewList() throws android.os.RemoteException {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                java.util.List<LocalMedia> _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    boolean _status = mRemote.transact(Stub.TRANSACTION_getPreviewList, _data, _reply, 0);
                    if (!_status) {
                        if (getDefaultImpl() != null) {
                            return getDefaultImpl().getPreviewList();
                        }
                    }
                    _reply.readException();
                    _result = _reply.createTypedArrayList(LocalMedia.CREATOR);
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            @Override
            public int getPosition() throws android.os.RemoteException {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                int _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    boolean _status = mRemote.transact(Stub.TRANSACTION_getPosition, _data, _reply, 0);
                    if (!_status) {
                        if (getDefaultImpl() != null) {
                            return getDefaultImpl().getPosition();
                        }
                    }
                    _reply.readException();
                    _result = _reply.readInt();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            public static IPreviewListData sDefaultImpl;
        }

        static final int TRANSACTION_getPreviewList = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
        static final int TRANSACTION_getPosition = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);

        public static boolean setDefaultImpl(IPreviewListData impl) {
            // Only one user of this interface can use this function
            // at a time. This is a heuristic to detect if two different
            // users in the same process use this function.
            if (Proxy.sDefaultImpl != null) {
                throw new IllegalStateException("setDefaultImpl() called twice");
            }
            if (impl != null) {
                Proxy.sDefaultImpl = impl;
                return true;
            }
            return false;
        }

        public static IPreviewListData getDefaultImpl() {
            return Proxy.sDefaultImpl;
        }
    }

    public static final String DESCRIPTOR = "com.yy.ourtime.photoalbum.IPreviewListData";

    public java.util.List<LocalMedia> getPreviewList() throws android.os.RemoteException;

    public int getPosition() throws android.os.RemoteException;
}
