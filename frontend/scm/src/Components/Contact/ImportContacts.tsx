"use client";
import React, { useState, ChangeEvent } from 'react';
import { toast } from 'react-toastify';
import { useRouter } from 'next/navigation';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faUpload } from '@fortawesome/free-solid-svg-icons';
import 'react-toastify/dist/ReactToastify.css';

interface ImportContactsProps {
    tenantUniqueName: string;
    IdToken: string;
    onClose?: () => void;
}

const ImportContacts: React.FC<ImportContactsProps> = ({ tenantUniqueName, IdToken, onClose }) => {
    const router = useRouter();
    const [showPopup, setShowPopup] = useState(false);
    const [files, setFiles] = useState<FileList | null>(null); // Za več datotek
    const [requestLoading, setRequestLoading] = useState<boolean>(false);

    const handleFileChange = (e: ChangeEvent<HTMLInputElement>) => {
        if (e.target.files) {
            setFiles(e.target.files);
        }
    };

    const handleImport = async () => {
        if (!files || files.length === 0) {
            toast.error('Please select at least one file to upload');
            return;
        }

        setRequestLoading(true);

        try {
            for (const file of Array.from(files)) {
                let apiUrl = '/contacts/import-excel';

                if (file.name.endsWith('.json')) {
                    const fileContent = await file.text();
                    const jsonData = JSON.parse(fileContent);

                    //If type is atribute in data then call endpoint for registration
                    apiUrl = jsonData.type ? '/contacts/import-json/registration' : '/contacts/import-json';
                }

                const formData = new FormData();
                formData.append('file', file);
                formData.append('tenantUniqueName', tenantUniqueName);

                const res = await fetch(`${process.env.NEXT_PUBLIC_API_URL}${apiUrl}`, {
                    method: 'POST',
                    headers: {
                        'userToken': `Bearer ${IdToken}`,
                    },
                    body: formData,
                });

                if (!res.ok) {
                    toast.error(`Failed to import contacts from ${file.name}: ${res.statusText}`);
                } else {
                    toast.success(`Contacts from ${file.name} imported successfully`);
                }
            }

            if (onClose) onClose();
            router.refresh();
            setShowPopup(false);
            setRequestLoading(false);

        } catch (error: any) {
            toast.error(error.message || 'Failed to import contacts');
            setShowPopup(false);
            setRequestLoading(false);
        }
    };

    const openPopup = () => {
        setShowPopup(true);
    };

    return (
        <div>
            <button onClick={openPopup}
                    className="btn px-4 btn-sm bg-primary border-0 text-white rounded-8 font-semibold hover:scale-105 transition hover:bg-primary ml-3">
                Import
                <FontAwesomeIcon className="ml-1 w-3.5 h-auto" icon={faUpload} />
            </button>

            {showPopup && (
                <div className="fixed z-20 flex flex-col justify-center items-center bg-gray-500 bg-opacity-65 inset-0">
                    <div className="bg-white p-10 rounded-8 shadow-lg max-w-3xl w-full my-10 overflow-auto">
                        <h2 className="font-semibold text-2xl">Import Contacts</h2>
                        <p className="font-light text-md mb-2">Select file(s) to import contacts. Supported formats: .xlsx, .xls, .json</p>
                        <p className="font-light text-sm mb-2">File(s) should follow this structure and rules:</p>
                        <ul className="list-disc list-inside text-gray-700 text-sm mb-10">
                            <li className="mb-2">This columns are optional, but they are presets for all tenants:
                                <b> Title, prefix, name, lastname, phoneNumber, email, address, houseNumber, company,
                                    city, postNumber, country, comment</b>
                            </li>
                            <li className="mb-2">
                                In case <b>Title, name, lastname</b> columns are present, Title will be chosen over name and lastname, otherwise name and/or lastname will be used as title.
                            </li>
                            <li className="mb-2">Other column names will be <b>added as new properties</b> for the contact.</li>
                            <li className="mb-2">If the column value matches the column name, the value will be <b>set as a contact tag</b>.</li>
                            <li className="mb-2">If the column value <b>is empty it will be ignored</b> and skipped.</li>
                        </ul>

                        <form>
                            <div className="mb-4">
                                <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="file">
                                    File(s)*
                                </label>
                                <input
                                    type="file"
                                    id="file"
                                    accept=".xlsx, .xls, .json"
                                    multiple
                                    onChange={handleFileChange}
                                    className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline"
                                />
                            </div>
                            <div className="mt-4 flex justify-center items-center">
                                <button onClick={() => {
                                    setShowPopup(false);
                                    if (onClose) onClose();
                                }}
                                        className="mt-4 mx-1 px-4 py-1 bg-danger text-white rounded-8 font-semibold hover:bg-danger hover:scale-105 transition">
                                    Close Popup
                                </button>
                                {requestLoading ? (
                                    <span className="loading loading-spinner text-primary"></span>
                                ) : (
                                    <button type="button" onClick={handleImport}
                                            className="mt-4 mx-1 px-4 py-1 bg-primary text-white rounded-8 font-semibold hover:bg-primary hover:scale-105 transition">
                                        Import Contacts
                                    </button>
                                )}
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
};

export default ImportContacts;