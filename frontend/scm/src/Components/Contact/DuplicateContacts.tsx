"use client";

import React, { useEffect, useState } from 'react';
import { Contact as ContactModel } from '@/models/Contact';
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import {
    faPlus,
    faMinus,
    faTag,
    faClock,
    faCommentDots,
    faArrowLeft,
    faHouse,
    faAddressBook, faCodeMerge
} from "@fortawesome/free-solid-svg-icons";
import { toast } from 'react-toastify';
import { useRouter } from 'next/navigation';
import { Tenant as TenantModel } from '@/models/Tenant';
import Link from "next/link";

type DuplicateContactsProps = {
    IdToken: string;
    tenantUniqueName: string;
};

const fetchDuplicateContacts = async (tenantUniqueName: string, IdToken: string): Promise<{ [title: string]: ContactModel[] }> => {
    try {
        const res = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/contacts/duplicates/${tenantUniqueName}`, {
            headers: {
                'userToken': `Bearer ${IdToken}`,
            },
        });

        if (!res.ok) {
            toast.error(res.statusText || 'Failed to fetch duplicated contacts');
            return {};
        }

        return await res.json();
    } catch (error: any) {
        toast.error(error.message || 'Failed to fetch duplicated contacts');
        return {};
    }
};

const fetchTenant = async (tenantUniqueName: string, IdToken: string): Promise<TenantModel | null> => {
    try {
        const res = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/tenants/unique/${tenantUniqueName}`, {
            headers: {
                'userToken': `Bearer ${IdToken}`,
            },
        });

        if (!res.ok) {
            toast.error(res.statusText || 'Failed to fetch tenant labels');
            return null;
        }

        return await res.json();
    } catch (error: any) {
        toast.error(error.message || 'Failed to fetch tenant labels');
        return null;
    }
};

const mergeContacts = async (targetContactId: string, sourceContactId: string, tenantUniqueName: string, IdToken: string) => {
    try {
        const res = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/contacts/merge?targetContactId=${targetContactId}&sourceContactId=${sourceContactId}&tenantUniqueName=${tenantUniqueName}`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "userToken": `Bearer ${IdToken}`,
            }
        });

        if (!res.ok) {
            toast.error(res.statusText || 'Failed to merge contacts');
            return false;
        }

        toast.success('Contacts merged successfully');
        return true;
    } catch (error: any) {
        toast.error(error.message || 'Failed to merge contacts');
        return false;
    }
};

const DuplicateContacts: React.FC<DuplicateContactsProps> = ({ IdToken, tenantUniqueName }) => {
    const router = useRouter();
    const [duplicateContacts, setDuplicateContacts] = useState<{ [title: string]: ContactModel[] }>({});
    const [expandedRows, setExpandedRows] = useState<string[]>([]);
    const [loading, setLoading] = useState<boolean>(true);
    const [tenant, setTenant] = useState<TenantModel | null>(null); // Tenant labels
    const [showMergeModal, setShowMergeModal] = useState(false); // Modal state
    const [mergeDetails, setMergeDetails] = useState<{ targetContact: ContactModel, sourceContact: ContactModel } | null>(null);

    useEffect(() => {
        fetchDuplicateContacts(tenantUniqueName, IdToken).then((contacts) => {
            setDuplicateContacts(contacts);
            setLoading(false);
        });

        fetchTenant(tenantUniqueName, IdToken).then((tenantData) => {
            setTenant(tenantData);
        });
    }, [tenantUniqueName, IdToken]);

    const toggleRow = (title: string) => {
        setExpandedRows((prevExpandedRows) =>
            prevExpandedRows.includes(title)
                ? prevExpandedRows.filter((row) => row !== title)
                : [...prevExpandedRows, title]
        );
    };

    const compareProps = (targetProps: { [key: string]: string }, sourceProps: { [key: string]: string }) => {
        const allKeys = new Set([...Object.keys(targetProps), ...Object.keys(sourceProps)]);
        const diffProps: { [key: string]: { target?: string, source: string } } = {};

        allKeys.forEach((key) => {
            const targetValue = targetProps[key];
            const sourceValue = sourceProps[key];

            if (!targetValue) {
                diffProps[key] = { source: sourceValue };
            } else if (targetValue !== sourceValue) {
                diffProps[key] = { target: targetValue, source: sourceValue };
            }
        });

        return diffProps;
    };

    const compareTags = (targetTags: string[], sourceTags: string[]) => {
        const diffTags: { addedTags: string[], removedTags: string[] } = { addedTags: [], removedTags: [] };

        sourceTags.forEach(tag => {
            if (!targetTags.includes(tag)) {
                diffTags.addedTags.push(tag);
            }
        });

        targetTags.forEach(tag => {
            if (!sourceTags.includes(tag)) {
                diffTags.removedTags.push(tag);
            }
        });

        return diffTags;
    };

    const openMergeModal = (targetContact: ContactModel, sourceContact: ContactModel) => {
        setMergeDetails({ targetContact, sourceContact });
        setShowMergeModal(true);
    };

    const handleMerge = async () => {
        if (mergeDetails) {
            const success = await mergeContacts(mergeDetails.targetContact.id, mergeDetails.sourceContact.id, tenantUniqueName, IdToken);
            if (success) {
                setDuplicateContacts((prevContacts) => {
                    const updatedContacts = { ...prevContacts };
                    delete updatedContacts[mergeDetails.targetContact.id];
                    return updatedContacts;
                });
                setShowMergeModal(false);
                router.push('/contacts/' + tenantUniqueName);
            }
        }
    };

    const formatDate = (dateString: Date | undefined) => {
        if (dateString !== undefined) {
            const date = new Date(dateString);
            const formattedDate = date.toLocaleString('sl-SI', {
                timeZone: 'Europe/Ljubljana',
                day: '2-digit',
                month: '2-digit',
                year: 'numeric',
                hour: '2-digit',
                minute: '2-digit',
                second: '2-digit',
                hour12: false,
            });
            return formattedDate.replace(',', '');
        }
    };

    return (
        <>
        <div className={"flex items-center"}>
            <FontAwesomeIcon
                icon={faArrowLeft}
                className="text-primary mr-4 cursor-pointer w-3.5 h-auto"
                onClick={() => router.back()}
            />
            <div className="text-sm breadcrumbs mx-2">
                <ul className={"text-gray-500"}>
                    <li><Link
                        href={"/"}><FontAwesomeIcon icon={faHouse} className={"mr-1"}/>Home</Link></li>
                    <li><Link
                        href={`/contacts/${tenantUniqueName}`}><FontAwesomeIcon icon={faAddressBook}
                                                                                             className={"mr-1"}/>{tenant?.title}
                    </Link></li>
                    <li><Link
                        href={"#"}><FontAwesomeIcon icon={faCodeMerge} className={"mr-1"}/>Duplicated contacts</Link></li>
                </ul>
            </div>
        </div>
    <div className="container mx-auto p-4">
        <h2 className="text-3xl text-primary font-semibold mb-4">Duplicated Contacts</h2>
        {loading ? (
            <p>Loading duplicated contacts...</p>
        ) : Object.keys(duplicateContacts).length === 0 ? (
            <p>There are no duplicated contacts.</p>
        ) : (
            <table className="min-w-full bg-white border border-gray-200">
                <tbody>
                {Object.keys(duplicateContacts).map((title) => (
                    <React.Fragment key={title}>
                        <tr>
                            <td className="py-2 px-4 border-b text-center">{title}</td>
                            <td className="py-2 px-4 border-b text-right">
                                <button
                                    className="btn px-4 btn-sm bg-primary border-0 text-white rounded-8 font-semibold hover:scale-105 transition hover:bg-primary"
                                    onClick={() => toggleRow(title)}
                                >
                                        {expandedRows.includes(title) ? (
                                            <>
                                                <span className="mr-2">Show less</span>
                                                <FontAwesomeIcon icon={faMinus} />
                                            </>
                                        ) : (
                                            <>
                                                <span className="mr-2">Show more</span>
                                                <FontAwesomeIcon icon={faPlus} />
                                            </>
                                        )}
                                    </button>
                                </td>
                            </tr>
                            {expandedRows.includes(title) && (
                                <tr>
                                    <td colSpan={2}>
                                        <div className="p-4 bg-gray-100 rounded">
                                            <div className="text-center font-bold text-xl mb-4">{title}</div>
                                            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                                                {duplicateContacts[title].map((contact, index) => {
                                                    const otherContact = duplicateContacts[title][1 - index];
                                                    const diffProps = compareProps(contact.props, otherContact.props);
                                                    return (
                                                        <div key={contact.id} className="relative p-4 bg-white rounded shadow-sm">
                                                            <div className="mb-4 text-lg font-bold">{contact.title}</div>
                                                            <div className="flex items-center mb-2">
                                                                <FontAwesomeIcon icon={faCommentDots} className="mr-2 text-primary" />
                                                                <strong>Comments:&nbsp;</strong> {contact.comments || 'No comments'}
                                                            </div>
                                                            <div className="flex items-center mb-2">
                                                                <FontAwesomeIcon icon={faClock} className="mr-2 text-primary" />
                                                                <strong>Created At:&nbsp;</strong> {formatDate(contact.createdAt)}
                                                            </div>
                                                            <div className="mb-2">
                                                                <div className="flex items-center">
                                                                    <FontAwesomeIcon icon={faTag} className="mr-2 text-primary" />
                                                                    <strong>Tags:</strong>
                                                                </div>
                                                                <div className="flex flex-wrap gap-2 mt-2">
                                                                    {contact.tags.map((tag, i) => (
                                                                        <span
                                                                            key={i}
                                                                            className={`${
                                                                                otherContact.tags.includes(tag)
                                                                                    ? 'bg-white text-primary' 
                                                                                    : 'bg-blue-100 text-blue-800'
                                                                            } border border-primary text-sm font-medium px-3 py-1.5 rounded-8`}
                                                                        >
            {tag}
        </span>
                                                                    ))}
                                                                </div>
                                                            </div>
                                                            <div className="grid grid-cols-1 gap-4 mt-4 mb-12">
                                                                {Object.entries(contact.props).map(([key, value]) => (
                                                                    <div
                                                                        key={key}
                                                                        className={`p-4 rounded-8 shadow-sm ${
                                                                            diffProps[key]
                                                                                ? 'bg-blue-100 text-blue-800'
                                                                                : 'bg-gray-50 text-black'
                                                                        }`}
                                                                    >
                                                                        <h4 className="font-semibold text-lg">{tenant?.labels[key] || key}</h4>
                                                                        <p className="mt-1">{value}</p>
                                                                    </div>
                                                                ))}
                                                            </div>
                                                            <button
                                                                className="absolute bottom-4 btn px-4 btn-sm bg-primary border-0 text-white rounded-8 font-semibold hover:scale-105 transition hover:bg-primary"
                                                                onClick={() => openMergeModal(contact, otherContact)}
                                                            >
                                                                Choose This Contact
                                                            </button>
                                                        </div>
                                                    );
                                                })}
                                            </div>
                                        </div>
                                    </td>
                                </tr>
                            )}
                        </React.Fragment>
                    ))}
                    </tbody>
                </table>
            )}

            {showMergeModal && mergeDetails && (
                <div className="fixed z-20 flex flex-col justify-center items-center bg-gray-500 bg-opacity-65 inset-0">
                    <div className="bg-white p-10 rounded-8 shadow-lg max-w-3xl w-full my-10 h-full overflow-auto">
                        <h2 className="text-xl font-semibold mb-4">Confirm Merge</h2>
                        <p className="mb-4">The following changes will be applied:</p>

                        <div className="grid grid-cols-1 gap-4 mb-4">
                            {Object.keys(mergeDetails.sourceContact.props).map((key, index) => {
                                const targetProp = mergeDetails.targetContact.props[key];
                                const sourceProp = mergeDetails.sourceContact.props[key];

                                if (targetProp === sourceProp) {
                                    return null;
                                }

                                if (sourceProp === "" || sourceProp === undefined) {
                                    return null;
                                }

                                return (
                                    <div key={index} className="bg-gray-50 p-4 rounded-8 shadow-sm">
                                        <h4 className="font-semibold text-lg">{tenant?.labels[key] || key}</h4>
                                        <p className={`mt-1`}>
                                            {targetProp && sourceProp ? (
                                                <>
                                                    <span className="text-green-800">{targetProp}</span>
                                                    <span className="ml-2 text-red-800 line-through">{sourceProp}</span>
                                                </>
                                            ) : targetProp ? (
                                                <span className="text-green-800">{targetProp}</span>
                                            ) : (
                                                <span className="text-green-800">{sourceProp}</span>
                                            )}
                                        </p>
                                    </div>
                                );
                            })}
                        </div>

                        <div className="grid grid-cols-1 gap-4 mb-4">
                            {mergeDetails && compareTags(mergeDetails.targetContact.tags, mergeDetails.sourceContact.tags).addedTags.length > 0 && (
                                <div className="bg-gray-50 p-4 rounded-8 shadow-sm">
                                    <h4 className="font-semibold text-lg">Tags</h4>
                                    <div className="flex flex-wrap gap-2 mt-2">
                                        {compareTags(mergeDetails.targetContact.tags, mergeDetails.sourceContact.tags).addedTags.map((tag, index) => (
                                            <span
                                                key={index}
                                                className="bg-green-100 text-green-800 border border-green-500 text-sm font-medium px-3 py-1.5 rounded-8"
                                            >
                                    {tag}
                                </span>
                                        ))}
                                    </div>
                                </div>
                            )}
                        </div>

                        <div className="flex justify-center mt-4">
                            <button
                                className="mt-4 mx-1 px-4 py-1 bg-danger text-white rounded-8 font-semibold hover:bg-danger hover:scale-105 transition"
                                onClick={() => setShowMergeModal(false)}
                            >
                                Cancel
                            </button>
                            <button
                                className="mt-4 mx-1 px-4 py-1 bg-primary text-white rounded-8 font-semibold hover:bg-primary hover:scale-105 transition"
                                onClick={handleMerge}
                            >
                                Confirm Merge
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
        </>
    );
};

export default DuplicateContacts;